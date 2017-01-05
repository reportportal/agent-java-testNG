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
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.UpdateLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;

import static com.epam.reportportal.listeners.ListenersUtils.handleException;

/**
 * TestNG service implements operations for interaction report portal
 * 
 */
public class TestNGService implements ITestNGService {
	private final Logger logger = LoggerFactory.getLogger(TestNGService.class);

	public static final String ID = "id";
	public static final String NOT_ISSUE = "NOT_ISSUE";

	private BatchedReportPortalService reportPortalService;

	private TestNGContext testNGContext;

	private Mode launchRunningMode;
	private Set<String> tags;
	private String description;
	private boolean isSkippedAnIssue;

	@Inject
	public TestNGService(ListenerParameters parameters, BatchedReportPortalService service, TestNGContext testNGContext) {
		tags = parameters.getTags();
		description = parameters.getDescription();
		launchRunningMode = parameters.getMode();
		isSkippedAnIssue = parameters.getIsSkippedAnIssue();
		reportPortalService = service;
		this.testNGContext = testNGContext;
	}

	@Override
	public void startLaunch() {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(testNGContext.getLaunchName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setTags(tags);
		rq.setMode(launchRunningMode);
		if (description != null) {
			rq.setDescription(description);
		}
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startLaunch(rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable start the launch: '" + testNGContext.getLaunchName() + "'");
		}
		if (rs != null) {
			testNGContext.setLaunchID(rs.getId());
		}
	}


	@Override
	public void addTags(Set<String> newTags) {
		if (newTags.isEmpty()) {
			return;
		}
		UpdateLaunchRQ rq = new UpdateLaunchRQ();
		tags.addAll(newTags);
		rq.setTags(tags);
		try {
			reportPortalService.updateLaunch(testNGContext.getLaunchID(), rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable to update the launch tags: '" + testNGContext.getLaunchID() + "'");
		}
	}

	@Override
	public void finishLaunch() {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(testNGContext.getIsLaunchFailed() ? Statuses.FAILED : Statuses.PASSED);
		try {
			reportPortalService.finishLaunch(testNGContext.getLaunchID(), rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable finish the launch: '" + testNGContext.getLaunchID() + "'");
		}
	}

	@Override
	public void startTestSuite(ISuite suite) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setLaunchId(testNGContext.getLaunchID());
		rq.setName(suite.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("SUITE");
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startRootTestItem(rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable start test suite: '" + suite.getName() + "'");
		}
		if (rs != null) {
			suite.setAttribute(ID, rs.getId());
		}
	}

	@Override
	public void finishTestSuite(ISuite suite) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(getSuiteStatus(suite));
		try {
			reportPortalService.finishTestItem(String.valueOf(suite.getAttribute(ID)), rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable finish test suite: '" + String.valueOf(suite.getAttribute(ID)) + "'");
		}
	}

	@Override
	public void startTest(ITestContext testContext) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(testContext.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setLaunchId(testNGContext.getLaunchID());
		rq.setType("TEST");
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startTestItem(String.valueOf(testContext.getSuite().getAttribute(ID)), rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable start test: '" + testContext.getName() + "'");
		}
		if (rs != null) {
			testContext.setAttribute(ID, rs.getId());
		}
	}

	@Override
	public void finishTest(ITestContext testContext) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		String status = null;
		if (isTestPassed(testContext)) {
			status = Statuses.PASSED;
		} else {
			status = Statuses.FAILED;
		}
		rq.setStatus(status);
		try {
			reportPortalService.finishTestItem(String.valueOf(testContext.getAttribute(ID)), rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable finish test: '" + String.valueOf(testContext.getAttribute(ID)) + "'");
		}
	}

	@Override
	public void startTestMethod(ITestResult testResult) {
		if (testResult.getAttribute(ID) != null) {
			return;
		}
		StartTestItemRQ rq = new StartTestItemRQ();
		String testStepName = testResult.getMethod().getMethodName();
		rq.setName(testStepName);
		rq.setLaunchId(testNGContext.getLaunchID());

		rq.setDescription(createStepDescription(testResult));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(TestMethodType.getStepType(testResult.getMethod()).toString());
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startTestItem(String.valueOf(testResult.getTestContext().getAttribute(ID)), rq);
		} catch (Exception e) {
			handleException(e, logger, new StringBuilder("Unable start test method: '").append(testStepName).append("'").toString());
		}
		if (rs != null) {
			testResult.setAttribute(ID, rs.getId());
			ReportPortalListenerContext.setRunningNowItemId(rs.getId());
		}
	}

	@Override
	public void finishTestMethod(String status, ITestResult testResult) {
		ReportPortalListenerContext.setRunningNowItemId(null);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status);
		// Allows indicate that SKIPPED is not to investigate items for WS
		if (status.equals(Statuses.SKIPPED) && !isSkippedAnIssue) {
			Issue issue = new Issue();
			issue.setIssueType(NOT_ISSUE);
			rq.setIssue(issue);
		}
		try {
			reportPortalService.finishTestItem(String.valueOf(testResult.getAttribute(ID)), rq);
		} catch (Exception e) {
			handleException(e, logger, new StringBuilder("Unable finish test method: '").append(String.valueOf(testResult.getAttribute(ID)))
					.append("'").toString());
		}
	}

	@Override
	public void startConfiguration(ITestResult testResult) {
		StartTestItemRQ rq = new StartTestItemRQ();
		String configName = testResult.getMethod().getMethodName();
		rq.setName(configName);

		rq.setDescription(testResult.getMethod().getDescription());
		rq.setStartTime(Calendar.getInstance().getTime());
		TestMethodType type = TestMethodType.getStepType(testResult.getMethod());
		rq.setType(type.toString());
		rq.setLaunchId(testNGContext.getLaunchID());

		String parentId = getConfigParent(testResult, type);
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startTestItem(parentId, rq);
		} catch (Exception e) {
			handleException(e, logger, "Unable start configuration: '" + configName + "'");
		}
		if (rs != null) {
			testResult.setAttribute(ID, rs.getId());
			ReportPortalListenerContext.setRunningNowItemId(rs.getId());
		}
	}

	@Override
	public void sendReportPortalMsg(ITestResult result) {
		SaveLogRQ slrq = new SaveLogRQ();
		slrq.setTestItemId(String.valueOf(result.getAttribute(ID)));
		slrq.setLevel("ERROR");
		slrq.setLogTime(Calendar.getInstance().getTime());
		if (result.getThrowable() != null)
			slrq.setMessage(result.getThrowable().getClass().getName() + ": " + result.getThrowable().getMessage()
					+ System.getProperty("line.separator") + this.getStackTraceContext(result.getThrowable()));
		else
			slrq.setMessage("Just exception");
		slrq.setLogTime(Calendar.getInstance().getTime());
		try {
			reportPortalService.log(slrq);
		} catch (Exception e1) {
			handleException(e1, logger, "Unable to send message to Report Portal");
		}
	}

	private String getStackTraceContext(Throwable e) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < e.getStackTrace().length; i++) {
			result.append(e.getStackTrace()[i]);
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}

	private String createStepDescription(ITestResult testResult) {
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

	private String getSuiteStatus(ISuite suite) {
		Collection<ISuiteResult> suiteResults = suite.getResults().values();
		String suiteStatus = Statuses.PASSED;
		for (ISuiteResult suiteResult : suiteResults) {
			if (!(isTestPassed(suiteResult.getTestContext()))) {
				suiteStatus = Statuses.FAILED;
				break;
			}
		}
		// if at least one suite failed launch should be failed
		if (!testNGContext.getIsLaunchFailed()) {
			testNGContext.setIsLaunchFailed(suiteStatus.equals(Statuses.FAILED));
		}
		return suiteStatus;
	}

	/**
	 * Calculate parent id for configuration
	 */
	private String getConfigParent(ITestResult testResult, TestMethodType type) {
		String parentId = null;
		if (TestMethodType.BEFORE_SUITE.equals(type) || TestMethodType.AFTER_SUITE.equals(type)) {
			parentId = String.valueOf(testResult.getTestContext().getSuite().getAttribute(ID));
		} else {
			parentId = String.valueOf(testResult.getTestContext().getAttribute(ID));
		}
		return parentId;
	}

	/**
	 * Check is current method passed according the number of failed tests and
	 * configurations
	 * 
	 * @param testContext
	 * @return
	 */
	private boolean isTestPassed(ITestContext testContext) {
		return testContext.getFailedTests().size() == 0 && testContext.getFailedConfigurations().size() == 0
				&& testContext.getSkippedConfigurations().size() == 0 && testContext.getSkippedTests().size() == 0;
	}
}
