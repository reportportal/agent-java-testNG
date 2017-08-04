/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-testNG
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.testng.IAttributes;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import rp.com.google.common.base.Function;
import rp.com.google.common.base.Supplier;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * TestNG service implements operations for interaction report portal
 */
public class TestNGService implements ITestNGService {

    public static final String NOT_ISSUE = "NOT_ISSUE";
    public static final String RP_ID = "rp_id";

    private final Supplier<StartLaunchRQ> launchSupplier;
    private final ReportPortalClient reportPortalClient;
    private final ListenerParameters parameters;

    private ReportPortal reportPortal;


    private AtomicBoolean isLaunchFailed = new AtomicBoolean();

    public TestNGService(final ListenerParameters parameters, ReportPortalClient reportPortalClient) {
        this.parameters = parameters;
        this.reportPortalClient = reportPortalClient;

        this.launchSupplier = new Supplier<StartLaunchRQ>() {
            @Override
            public StartLaunchRQ get() {
                return buildStartLaunchRq(parameters);
            }
        };

    }

    @Override
    public void startLaunch() {
        StartLaunchRQ rq = launchSupplier.get();
        rq.setStartTime(Calendar.getInstance().getTime());
        this.reportPortal = ReportPortal.startLaunch(reportPortalClient, parameters, rq);
    }

    @Override
    public void finishLaunch() {
        FinishExecutionRQ rq = new FinishExecutionRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(isLaunchFailed.get() ? Statuses.FAILED : Statuses.PASSED);
        reportPortal.finishLaunch(rq);
    }

    /*
     * sometimes testng triggers on suite start several times.
     * This is why method is synchronized and check for attribute presence is added
     */
    @Override
    public synchronized void startTestSuite(ISuite suite) {
        //avoid starting same suite twice
        if (null == getAttribute(suite, RP_ID)) {
            StartTestItemRQ rq = buildStartSuiteRq(suite);
            final Maybe<String> item = reportPortal.startTestItem(rq);
            suite.setAttribute(RP_ID, item);
        }
    }

    @Override
    public synchronized void finishTestSuite(ISuite suite) {
        if (null != suite.getAttribute(RP_ID)) {
        /* 'real' end time */
            Date now = Calendar.getInstance().getTime();
            FinishTestItemRQ rq = new FinishTestItemRQ();
            rq.setEndTime(now);
            rq.setStatus(getSuiteStatus(suite));
            reportPortal.finishTestItem(this.<Maybe<String>>getAttribute(suite, RP_ID), rq);
            suite.removeAttribute(RP_ID);
        }

    }

    @Override
    public void startTest(ITestContext testContext) {
        StartTestItemRQ rq = buildStartTestItemRq(testContext);

        final Maybe<String> testID = reportPortal
                .startTestItem(this.<Maybe<String>>getAttribute(testContext.getSuite(), RP_ID), rq);

        testContext.setAttribute(RP_ID, testID);

    }

    @Override
    public void finishTest(ITestContext testContext) {
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(testContext.getEndDate());
        String status = isTestPassed(testContext) ? Statuses.PASSED : Statuses.FAILED;

        rq.setStatus(status);
        reportPortal.finishTestItem(this.<Maybe<String>>getAttribute(testContext, RP_ID), rq);

    }

    @Override
    public void startTestMethod(ITestResult testResult) {
        StartTestItemRQ rq = buildStartStepRq(testResult);
        if (rq == null)
            return;
        Maybe<String> stepMaybe = reportPortal
                .startTestItem(this.<Maybe<String>>getAttribute(testResult.getTestContext(), RP_ID), rq);

        testResult.setAttribute(RP_ID, stepMaybe);
    }

    @Override
    public void finishTestMethod(String status, ITestResult testResult) {
        final Date now = Calendar.getInstance().getTime();
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(now);
        rq.setStatus(status);
        // Allows indicate that SKIPPED is not to investigate items for WS
        if (status.equals(Statuses.SKIPPED) && !parameters.getSkippedAnIssue()) {
            Issue issue = new Issue();
            issue.setIssueType(NOT_ISSUE);
            rq.setIssue(issue);
        }

        reportPortal.finishTestItem(this.<Maybe<String>>getAttribute(testResult, RP_ID), rq);
    }

    @Override
    public void startConfiguration(ITestResult testResult) {
        TestMethodType type = TestMethodType.getStepType(testResult.getMethod());
        StartTestItemRQ rq = buildStartConfigurationRq(testResult, type);

        Maybe<String> parentId = getConfigParent(testResult, type);
        final Maybe<String> itemID = reportPortal.startTestItem(parentId, rq);
        testResult.setAttribute(RP_ID, itemID);
    }

    @Override
    public void sendReportPortalMsg(final ITestResult result) {
        ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
            @Override
            public SaveLogRQ apply(String itemId) {
                SaveLogRQ rq = new SaveLogRQ();
                rq.setTestItemId(itemId);
                rq.setLevel("ERROR");
                rq.setLogTime(Calendar.getInstance().getTime());
                if (result.getThrowable() != null) {
                    rq.setMessage(getStackTraceAsString(result.getThrowable()));
                } else
                    rq.setMessage("Test has failed without exception");
                rq.setLogTime(Calendar.getInstance().getTime());

                return rq;
            }
        });

    }

    /**
     * Extension point to customize suite creation event/request
     *
     * @param suite TestNG suite
     * @return Request to ReportPortal
     */
    protected StartTestItemRQ buildStartSuiteRq(ISuite suite) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(suite.getName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType("SUITE");
        return rq;
    }

    /**
     * Extension point to customize test creation event/request
     *
     * @param testContext TestNG test context
     * @return Request to ReportPortal
     */
    protected StartTestItemRQ buildStartTestItemRq(ITestContext testContext) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(testContext.getName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType("TEST");
        return rq;
    }

    /**
     * Extension point to customize launch creation event/request
     *
     * @param parameters Launch Configuration parameters
     * @return Request to ReportPortal
     */
    protected StartLaunchRQ buildStartLaunchRq(ListenerParameters parameters) {
        StartLaunchRQ rq = new StartLaunchRQ();
        rq.setName(parameters.getLaunchName());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setTags(parameters.getTags());
        rq.setMode(parameters.getLaunchRunningMode());
        if (!isNullOrEmpty(parameters.getDescription())) {
            rq.setDescription(parameters.getDescription());
        }
        return rq;
    }

    /**
     * Extension point to customize beforeXXX creation event/request
     *
     * @param testResult TestNG's testResult context
     * @param type       Type of method
     * @return Request to ReportPortal
     */
    protected StartTestItemRQ buildStartConfigurationRq(ITestResult testResult, TestMethodType type) {
        StartTestItemRQ rq = new StartTestItemRQ();
        String configName = testResult.getMethod().getMethodName();
        rq.setName(configName);

        rq.setDescription(testResult.getMethod().getDescription());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(type == null ? null : type.toString());
        return rq;
    }

    /**
     * Extension point to customize test step creation event/request
     *
     * @param testResult TestNG's testResult context
     * @return Request to ReportPortal
     */
    protected StartTestItemRQ buildStartStepRq(ITestResult testResult) {
        if (testResult.getAttribute(RP_ID) != null) {
            return null;
        }
        StartTestItemRQ rq = new StartTestItemRQ();
        String testStepName = testResult.getMethod().getMethodName();
        rq.setName(testStepName);

        rq.setDescription(createStepDescription(testResult));
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(TestMethodType.getStepType(testResult.getMethod()).toString());
        return rq;
    }

    /**
     * Extension point to customize test step description
     *
     * @param testResult TestNG's testResult context
     * @return Test/Step Description being sent to ReportPortal
     */
    protected String createStepDescription(ITestResult testResult) {
        StringBuilder stringBuffer = new StringBuilder();
        if (testResult.getMethod().getDescription() != null) {
            stringBuffer.append(testResult.getMethod().getDescription());
        }
        if (testResult.getParameters() != null && testResult.getParameters().length != 0) {
            stringBuffer.append(" [ ");
            for (Object parameter : testResult.getParameters()) {
                stringBuffer.append(" ");
                stringBuffer.append(parameter);
                stringBuffer.append(" |");
            }
            stringBuffer.deleteCharAt(stringBuffer.lastIndexOf("|"));
            stringBuffer.append(" ] ");
        }
        return stringBuffer.toString();
    }

    /**
     * Extension point to customize test suite status being sent to ReportPortal
     *
     * @param suite TestNG's suite
     * @return Status PASSED/FAILED/etc
     */
    protected String getSuiteStatus(ISuite suite) {
        Collection<ISuiteResult> suiteResults = suite.getResults().values();
        String suiteStatus = Statuses.PASSED;
        for (ISuiteResult suiteResult : suiteResults) {
            if (!(isTestPassed(suiteResult.getTestContext()))) {
                suiteStatus = Statuses.FAILED;
                break;
            }
        }
        // if at least one suite failed launch should be failed
        isLaunchFailed.compareAndSet(false, suiteStatus.equals(Statuses.FAILED));
        return suiteStatus;
    }

    /**
     * Check is current method passed according the number of failed tests and
     * configurations
     *
     * @param testContext TestNG's test content
     * @return TRUE if passed, FALSE otherwise
     */
    protected boolean isTestPassed(ITestContext testContext) {
        return testContext.getFailedTests().size() == 0 && testContext.getFailedConfigurations().size() == 0
                && testContext.getSkippedConfigurations().size() == 0 && testContext.getSkippedTests().size() == 0;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getAttribute(IAttributes attributes, String attribute) {
        return (T) attributes.getAttribute(attribute);
    }

    /**
     * Calculate parent id for configuration
     */
    private Maybe<String> getConfigParent(ITestResult testResult, TestMethodType type) {
        Maybe<String> parentId;
        if (TestMethodType.BEFORE_SUITE.equals(type) || TestMethodType.AFTER_SUITE.equals(type)) {
            parentId = getAttribute(testResult.getTestContext().getSuite(), RP_ID);
        } else {
            parentId = getAttribute(testResult.getTestContext(), RP_ID);
        }
        return parentId;
    }
}
