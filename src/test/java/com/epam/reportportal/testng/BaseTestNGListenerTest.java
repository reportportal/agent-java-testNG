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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

import static org.mockito.Mockito.*;

/**
 * @author Pavel Bortnik
 */
public class BaseTestNGListenerTest {

	@Mock
	private TestNGService testNGService;
	@Mock
	private ITestResult result;
	@Mock
	private ITestContext context;
	@Mock
	private ISuite suite;

	private BaseTestNGListener listener;



	@BeforeEach
	public void init() {
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
		listener.onStart(suite);
		verify(testNGService, times(1)).startTestSuite(eq(suite));
	}

	@Test
	public void testOnSuiteFinish() {
		listener.onFinish(suite);
		verify(testNGService, times(1)).finishTestSuite(eq(suite));
	}

	@Test
	public void testOnStart() {
		listener.onStart(context);
		verify(testNGService, times(1)).startTest(eq(context));
	}

	@Test
	public void testOnFinish() {
		listener.onFinish(context);
		verify(testNGService, times(1)).finishTest(eq(context));
	}

	@Test
	public void testOnTestStart() {
		listener.onTestStart(result);
		verify(testNGService, times(1)).startTestMethod(eq(result));
	}

	@Test
	public void testOnTestSuccess() {
		listener.onTestSuccess(result);
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.PASSED), eq(result));
	}

	@Test
	public void testOnTestFailure() {
		listener.onTestFailure(result);
		verify(testNGService, times(1)).sendReportPortalMsg(eq(result));
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.FAILED), eq(result));
	}

	@Test
	public void testOnTestSkipped() {
		listener.onTestSkipped(result);
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.SKIPPED), eq(result));
	}

	@Test
	public void testBeforeConfiguration() {
		listener.beforeConfiguration(result);
		verify(testNGService, times(1)).startConfiguration(eq(result));
	}

	@Test
	public void testOnConfigurationFailure() {
		listener.onConfigurationFailure(result);
		verify(testNGService, times(1)).sendReportPortalMsg(eq(result));
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.FAILED), eq(result));
	}

	@Test
	public void testOnConfigurationSuccess() {
		listener.onConfigurationSuccess(result);
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.PASSED), eq(result));
	}

	@Test
	public void testOnConfigurationSkip() {
		listener.onConfigurationSkip(result);
		verify(testNGService, times(1)).startConfiguration(eq(result));
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.SKIPPED), eq(result));
	}

	@Test
	public void testOnTestFailedButWithinSuccessPercentage() {
		listener.onTestFailedButWithinSuccessPercentage(result);
		verify(testNGService, times(1)).finishTestMethod(eq(Statuses.FAILED), eq(result));
	}

}
