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

import org.testng.*;
import org.testng.internal.IResultListener2;

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.listeners.Statuses;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Report portal custom event listener. Support executing parallel of test
 * methods, suites, test classes.
 * 
 */
public class ReportPortalTestNGListener implements IExecutionListener, ISuiteListener, IResultListener2 {

	private Supplier<ITestNGService> testNGService;

	// added to cover com.epam.reportportal.testng vulnerability
	private ThreadLocal<Boolean> isSuiteStarted;

	public ReportPortalTestNGListener() {
		isSuiteStarted = new ThreadLocal<Boolean>();
		isSuiteStarted.set(false);
		testNGService = Suppliers.memoize(new Supplier<ITestNGService>() {

			@Override
			public ITestNGService get() {
				return Injector.getInstance().getChildInjector(new TestNGListenersModule()).getBean(ITestNGService.class);
			}
		});
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
		// not implemented
	}
}
