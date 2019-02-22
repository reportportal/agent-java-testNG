/*
 * Copyright (C) 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.ResultMap;
import rp.com.google.common.base.Supplier;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Pavel Bortnik
 */
@SuppressWarnings("unchecked")
public class TestNGServiceTest {

	private static final String RP_ID = "rp_id";

	private TestNGService testNGService;

	@Mock
	private Launch launch;

	@Mock
	private ITestContext testContext;

	@Mock
	private ITestResult testResult;

	@Mock
	private ITestNGMethod method;

	@Mock
	private ISuite suite;

	@Mock
	private Maybe<String> id;

	@Before
	public void preconditions() {
		MockitoAnnotations.initMocks(this);

		testNGService = new TestNGService(new TestNGService.MemoizingSupplier<Launch>(new Supplier<Launch>() {
			@Override
			public Launch get() {
				return launch;
			}
		}));

		when(testResult.getTestContext()).thenReturn(testContext);
		when(testResult.getMethod()).thenReturn(method);
		when(testResult.getAttribute(RP_ID)).thenReturn(id);
		when(testContext.getSuite()).thenReturn(suite);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);
	}

	@Test
	public void startLaunch() {
		testNGService.startLaunch();
		verify(launch, times(1)).start();
	}

	@Test
	public void finishLaunch() {
		testNGService.finishLaunch();
		verify(launch, times(1)).finish(Mockito.any(FinishExecutionRQ.class));
	}

	@Test
	public void startTestSuite() {
		testNGService.startTestSuite(suite);
		verify(launch, times(1)).startTestItem(any(StartTestItemRQ.class));
		verify(suite, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void finishTestSuite() {
		when(suite.getAttribute(RP_ID)).thenReturn(id);
		testNGService.finishTestSuite(suite);
		verify(launch, times(1)).finishTestItem(eq(id), any(FinishTestItemRQ.class));
		verify(suite, times(1)).removeAttribute(RP_ID);
	}

	@Test
	public void finishTestSuitNull() {
		testNGService.finishTestSuite(suite);
		verifyZeroInteractions(launch);
	}

	@Test
	public void startTest() {
		when(testContext.getSuite()).thenReturn(suite);
		ITestNGMethod[] methods = new ITestNGMethod[] { method };
		when(testContext.getAllTestMethods()).thenReturn(methods);
		when(suite.getAttribute(RP_ID)).thenReturn(id);
		testNGService.startTest(testContext);

		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testContext, times(1)).setAttribute(eq(RP_ID), Matchers.any(Maybe.class));
	}

	@Test
	public void startTestWithoutMethods() {
		testNGService.startTest(testContext);
		verify(launch, never()).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testContext, never()).setAttribute(eq(RP_ID), Matchers.any(Maybe.class));
	}

	@Test
	public void finishTest() {
		ResultMap empty = new ResultMap();
		when(testContext.getSuite()).thenReturn(suite);
		ITestNGMethod[] methods = new ITestNGMethod[] { method };
		when(testContext.getAllTestMethods()).thenReturn(methods);
		when(testContext.getFailedTests()).thenReturn(empty);
		when(testContext.getFailedConfigurations()).thenReturn(empty);
		when(testContext.getSkippedConfigurations()).thenReturn(empty);
		when(testContext.getSkippedTests()).thenReturn(empty);

		testNGService.finishTest(testContext);
		verify(launch, times(1)).finishTestItem(eq(id), any(FinishTestItemRQ.class));
	}

	@Test
	public void finishTestWithoutMethods() {
		testNGService.finishTest(testContext);
		verify(launch, never()).finishTestItem(eq(id), any(FinishTestItemRQ.class));
	}

	@Test
	public void startTestMethod() {
		ITestNGMethod method = mock(ITestNGMethod.class);
		when(method.isTest()).thenReturn(true);
		when(testResult.getMethod()).thenReturn(method);

		testNGService.startTestMethod(testResult);
		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testResult, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void finishTestMethod() {
		testNGService.finishTestMethod(Statuses.PASSED, testResult);
		verify(launch, times(1)).finishTestItem(eq(id), any(FinishTestItemRQ.class));
	}

	@Test
	public void finishTestMethodSkipped() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setSkippedAnIssue(true);
		when(launch.getParameters()).thenReturn(listenerParameters);
		when(testResult.getAttribute(RP_ID)).thenReturn(null);
		ITestNGMethod method = mock(ITestNGMethod.class);
		when(method.isTest()).thenReturn(true);
		when(testResult.getMethod()).thenReturn(method);

		testNGService.finishTestMethod(Statuses.SKIPPED, testResult);

		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testResult, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
		verify(launch, times(1)).finishTestItem(any(Maybe.class), any(FinishTestItemRQ.class));
	}

	@Test
	public void startConfiguration() {
		testNGService.startConfiguration(testResult);
		verify(launch, times(1)).startTestItem(any(Maybe.class), any(StartTestItemRQ.class));
		verify(testResult, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void testParentConfigSuite() {
		when(suite.getAttribute(RP_ID)).thenReturn(id);
		Maybe<String> configParent = testNGService.getConfigParent(testResult, TestMethodType.BEFORE_SUITE);
		Assert.assertThat("Incorrect id", configParent, is(id));
	}

	@Test
	public void testParentConfig() {
		Maybe<String> configParent = testNGService.getConfigParent(testResult, TestMethodType.STEP);
		Assert.assertThat("Incorrect id", configParent, is(id));
	}

}
