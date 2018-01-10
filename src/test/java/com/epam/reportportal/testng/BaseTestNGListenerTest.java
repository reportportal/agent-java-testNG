/*
 * Copyright 2017 EPAM Systems
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
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
 *
 */

package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.Statuses;
import org.mockito.Mockito;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Pavel Bortnik
 */
public class BaseTestNGListenerTest {

	private TestNGService testNGService;

	private BaseTestNGListener listener;

	@BeforeMethod
	public void init() {
		testNGService = Mockito.mock(TestNGService.class);
		listener = new BaseTestNGListener(testNGService);
	}

	@Test
	public void onExecutionStart() {
		listener.onExecutionStart();
		verify(testNGService, times(1)).startLaunch();
	}

	@Test
	public void onExecutionFinish() {
		listener.onExecutionFinish();
		verify(testNGService, times(1)).finishLaunch();
	}

	@Test
	public void onSuiteStart() {
		listener.onStart(any(ISuite.class));
		verify(testNGService, times(1)).startTestSuite(any(ISuite.class));
	}

	@Test
	public void onSuiteFinish() {
		listener.onFinish(any(ISuite.class));
		verify(testNGService, times(1)).finishTestSuite(any(ISuite.class));
	}

	@Test
	public void onStart() {
		listener.onStart(any(ITestContext.class));
		verify(testNGService, times(1)).startTest(any(ITestContext.class));
	}

	@Test
	public void onFinish() {
		listener.onFinish(any(ITestContext.class));
		verify(testNGService, times(1)).finishTest(any(ITestContext.class));
	}

	@Test
	public void onTestStart() {
		listener.onTestStart(any(ITestResult.class));
		verify(testNGService, times(1)).startTestMethod(any(ITestResult.class));
	}

	@Test
	public void onTestSuccess() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestSuccess(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.PASSED, testResult);
	}

	@Test
	public void onTestFailure() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestFailure(testResult);
		verify(testNGService, times(1)).sendReportPortalMsg(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.FAILED, testResult);
	}

	@Test
	public void onTestSkipped() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestSkipped(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.SKIPPED, testResult);
	}

	@Test
	public void beforeConfiguration() {
		listener.beforeConfiguration(any(ITestResult.class));
		verify(testNGService, times(1)).startConfiguration(any(ITestResult.class));
	}

	@Test
	public void onConfigurationFailure() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onConfigurationFailure(testResult);
		verify(testNGService, times(1)).sendReportPortalMsg(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.FAILED, testResult);
	}

	@Test
	public void onConfigurationSuccess() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onConfigurationSuccess(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.PASSED, testResult);
	}

	@Test
	public void onConfigurationSkip() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onConfigurationSkip(testResult);
		verify(testNGService, times(1)).startConfiguration(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.SKIPPED, testResult);
	}

	@Test
	public void onTestFailedButWithinSuccessPercentage() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestFailedButWithinSuccessPercentage(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.FAILED, testResult);
	}

}
