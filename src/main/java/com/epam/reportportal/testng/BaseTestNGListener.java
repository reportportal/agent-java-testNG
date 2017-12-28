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

import com.epam.reportportal.listeners.Statuses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;
import org.testng.internal.IResultListener2;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Report portal custom event listener. Support parallel execution of test
 * methods, suites, test classes.
 * Can be extended by providing {@link ITestNGService} implementation
 */
public class BaseTestNGListener implements IExecutionListener, ISuiteListener, IResultListener2 {

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseTestNGListener.class);

	private static final AtomicInteger INSTANCES = new AtomicInteger(0);

	private ITestNGService testNGService;

	public BaseTestNGListener(ITestNGService testNgService) {
		this.testNGService = testNgService;
		if (INSTANCES.incrementAndGet() > 1) {
			final String warning = "WARNING! More than one ReportPortal listener is added";
			LOGGER.warn(warning);

			//even if logger is not configured, print the message to default stdout
			System.out.println(warning);
		}
	}

	@Override
	public void onExecutionStart() {
		testNGService.startLaunch();
	}

	@Override
	public void onExecutionFinish() {
		testNGService.finishLaunch();
	}

	@Override
	public void onStart(ISuite suite) {
		testNGService.startTestSuite(suite);
	}

	@Override
	public void onFinish(ISuite suite) {
		testNGService.finishTestSuite(suite);
	}

	@Override
	public void onStart(ITestContext testContext) {
		testNGService.startTest(testContext);
	}

	@Override
	public void onFinish(ITestContext testContext) {
		testNGService.finishTest(testContext);
	}

	@Override
	public void onTestStart(ITestResult testResult) {
		testNGService.startTestMethod(testResult);
	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		testNGService.finishTestMethod(Statuses.PASSED, testResult);
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		testNGService.sendReportPortalMsg(testResult);
		testNGService.finishTestMethod(Statuses.FAILED, testResult);
	}

	@Override
	public void onTestSkipped(ITestResult testResult) {
		testNGService.finishTestMethod(Statuses.SKIPPED, testResult);
	}

	@Override
	public void beforeConfiguration(ITestResult testResult) {
		testNGService.startConfiguration(testResult);
	}

	@Override
	public void onConfigurationFailure(ITestResult testResult) {
		testNGService.sendReportPortalMsg(testResult);
		testNGService.finishTestMethod(Statuses.FAILED, testResult);
	}

	@Override
	public void onConfigurationSuccess(ITestResult testResult) {
		testNGService.finishTestMethod(Statuses.PASSED, testResult);
	}

	@Override
	public void onConfigurationSkip(ITestResult testResult) {
		testNGService.startConfiguration(testResult);
		testNGService.finishTestMethod(Statuses.SKIPPED, testResult);
	}

	// this action temporary doesn't supported by report portal
	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		testNGService.finishTestMethod(Statuses.FAILED, result);
	}
}
