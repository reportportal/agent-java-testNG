/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
