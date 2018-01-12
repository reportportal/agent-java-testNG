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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Pavel Bortnik
 */
public class BaseTestNGListenerTest {

	private TestNGService testNGService;

	private BaseTestNGListener listener;

	@Before
	public void init() {
		testNGService = Mockito.mock(TestNGService.class);
		listener = new BaseTestNGListener(testNGService);
	}

	@Test
	public void testOnExecutionStart() {
		listener.onExecutionStart();
		verify(testNGService, times(1)).startLaunch();
	}

	@Test
	public void testOnExecutionFinish() {
		listener.onExecutionFinish();
		verify(testNGService, times(1)).finishLaunch();
	}

	@Test
	public void testOnSuiteStart() {
		listener.onStart(any(ISuite.class));
		verify(testNGService, times(1)).startTestSuite(any(ISuite.class));
	}

	@Test
	public void testOnSuiteFinish() {
		listener.onFinish(any(ISuite.class));
		verify(testNGService, times(1)).finishTestSuite(any(ISuite.class));
	}

	@Test
	public void testOnStart() {
		listener.onStart(any(ITestContext.class));
		verify(testNGService, times(1)).startTest(any(ITestContext.class));
	}

	@Test
	public void testOnFinish() {
		listener.onFinish(any(ITestContext.class));
		verify(testNGService, times(1)).finishTest(any(ITestContext.class));
	}

	@Test
	public void testOnTestStart() {
		listener.onTestStart(any(ITestResult.class));
		verify(testNGService, times(1)).startTestMethod(any(ITestResult.class));
	}

	@Test
	public void testOnTestSuccess() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestSuccess(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.PASSED, testResult);
	}

	@Test
	public void testOnTestFailure() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestFailure(testResult);
		verify(testNGService, times(1)).sendReportPortalMsg(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.FAILED, testResult);
	}

	@Test
	public void testOnTestSkipped() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestSkipped(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.SKIPPED, testResult);
	}

	@Test
	public void testBeforeConfiguration() {
		listener.beforeConfiguration(any(ITestResult.class));
		verify(testNGService, times(1)).startConfiguration(any(ITestResult.class));
	}

	@Test
	public void testOnConfigurationFailure() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onConfigurationFailure(testResult);
		verify(testNGService, times(1)).sendReportPortalMsg(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.FAILED, testResult);
	}

	@Test
	public void testOnConfigurationSuccess() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onConfigurationSuccess(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.PASSED, testResult);
	}

	@Test
	public void testOnConfigurationSkip() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onConfigurationSkip(testResult);
		verify(testNGService, times(1)).startConfiguration(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.SKIPPED, testResult);
	}

	@Test
	public void testOnTestFailedButWithinSuccessPercentage() {
		ITestResult testResult = mock(ITestResult.class);
		listener.onTestFailedButWithinSuccessPercentage(testResult);
		verify(testNGService, times(1)).finishTestMethod(Statuses.FAILED, testResult);
	}

}
