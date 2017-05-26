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

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.listeners.Statuses;
import rp.com.google.inject.Module;
import org.testng.IExecutionListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.internal.IResultListener2;
import rp.com.google.common.base.Supplier;

import static rp.com.google.common.base.Suppliers.memoize;

/**
 * Report portal custom event listener. Support parallel execution of test
 * methods, suites, test classes.
 * Can be extended by providing {@link ITestNGService} implementation
 * or configured {@link Injector}
 */
public class BaseTestNGListener implements IExecutionListener, ISuiteListener, IResultListener2 {

    private Supplier<ITestNGService> testNGService;

    // added to cover com.epam.reportportal.testng vulnerability
    private static ThreadLocal<Boolean> isSuiteStarted = new ThreadLocal<Boolean>();

    public BaseTestNGListener() {
        this(new TestNGAgentModule());
    }

    public BaseTestNGListener(final Module... extensions) {
        this(Injector.createDefault(extensions));
    }

    public BaseTestNGListener(final Injector injector) {
        this(new Supplier<ITestNGService>() {
            @Override
            public ITestNGService get() {
                return injector.getBean(ITestNGService.class);
            }
        });
    }

    public BaseTestNGListener(final Supplier<ITestNGService> testNgService) {
        isSuiteStarted.set(false);
        testNGService = memoize(testNgService);
    }

    @Override
    public void onExecutionStart() {
        testNGService.get().startLaunch();
    }

    @Override
    public void onExecutionFinish() {
        testNGService.get().finishLaunch();
    }

    @Override
    public void onStart(ISuite suite) {
        // added to cover com.epam.reportportal.testng vulnerability
        if (!isSuiteStarted.get()) {
            testNGService.get().startTestSuite(suite);
            isSuiteStarted.set(true);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        if (isSuiteStarted.get()) {
            testNGService.get().finishTestSuite(suite);
            isSuiteStarted.set(false);
        }
    }

    @Override
    public void onStart(ITestContext testContext) {
        testNGService.get().startTest(testContext);
    }

    @Override
    public void onFinish(ITestContext testContext) {
        testNGService.get().finishTest(testContext);
    }

    @Override
    public void onTestStart(ITestResult testResult) {
        testNGService.get().startTestMethod(testResult);
    }

    @Override
    public void onTestSuccess(ITestResult testResult) {
        testNGService.get().finishTestMethod(Statuses.PASSED, testResult);
    }

    @Override
    public void onTestFailure(ITestResult testResult) {
        testNGService.get().sendReportPortalMsg(testResult);
        testNGService.get().finishTestMethod(Statuses.FAILED, testResult);
    }

    @Override
    public void onTestSkipped(ITestResult testResult) {
        testNGService.get().startTestMethod(testResult);
        testNGService.get().finishTestMethod(Statuses.SKIPPED, testResult);
    }

    @Override
    public void beforeConfiguration(ITestResult testResult) {
        testNGService.get().startConfiguration(testResult);
    }

    @Override
    public void onConfigurationFailure(ITestResult testResult) {
        testNGService.get().sendReportPortalMsg(testResult);
        testNGService.get().finishTestMethod(Statuses.FAILED, testResult);
    }

    @Override
    public void onConfigurationSuccess(ITestResult testResult) {
        testNGService.get().finishTestMethod(Statuses.PASSED, testResult);
    }

    @Override
    public void onConfigurationSkip(ITestResult testResult) {
        testNGService.get().startConfiguration(testResult);
        testNGService.get().finishTestMethod(Statuses.SKIPPED, testResult);
    }

    // this action temporary doesn't supported by report portal
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        testNGService.get().finishTestMethod(Statuses.FAILED, result);
    }
}
