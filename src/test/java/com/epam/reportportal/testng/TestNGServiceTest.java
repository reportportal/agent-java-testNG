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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.ResultMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

	@Mock
	private Maybe<OperationCompletionRS> complete;

	@Mock
	private StepReporter stepReporter;

	@BeforeEach
	public void preconditions() {
		testNGService = new TestNGService(new TestNGService.MemorizingSupplier<>(() -> launch));
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
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(launch.startTestItem(any())).thenReturn(id);

		testNGService.startTestSuite(suite);
		verify(launch, times(1)).startTestItem(any(StartTestItemRQ.class));
		verify(suite, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void finishTestSuite() {
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(suite.getAttribute(RP_ID)).thenReturn(id);

		testNGService.finishTestSuite(suite);
		verify(launch, times(1)).finishTestItem(eq(id), any(FinishTestItemRQ.class));
		verify(suite, times(1)).removeAttribute(RP_ID);
	}

	@Test
	public void finishTestSuitNull() {
		when(launch.getParameters()).thenReturn(new ListenerParameters());

		testNGService.finishTestSuite(suite);
		verify(launch, times(1)).getParameters();
	}

	@Test
	public void startTest() {
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(launch.startTestItem(any(Maybe.class), any())).thenReturn(id);
		when(testContext.getSuite()).thenReturn(suite);
		when(testContext.getSuite()).thenReturn(suite);
		ITestNGMethod[] methods = new ITestNGMethod[] { method };
		when(testContext.getAllTestMethods()).thenReturn(methods);
		when(suite.getAttribute(RP_ID)).thenReturn(id);
		testNGService.startTest(testContext);

		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testContext, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void startTestWithoutMethods() {
		testNGService.startTest(testContext);
		verify(launch, never()).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testContext, never()).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void finishTest() {
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(testContext.getAttribute(RP_ID)).thenReturn(id);
		ResultMap empty = new ResultMap();
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
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(launch.startTestItem(any(Maybe.class), any())).thenReturn(id);
		when(testResult.getTestContext()).thenReturn(testContext);
		when(testResult.getMethod()).thenReturn(method);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);
		when(method.isTest()).thenReturn(true);

		testNGService.startTestMethod(testResult);
		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testResult, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void finishTestMethod() {
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(testResult.getAttribute(RP_ID)).thenReturn(id);
		when(launch.getStepReporter()).thenReturn(stepReporter);

		testNGService.finishTestMethod(Statuses.PASSED, testResult);
		verify(launch, times(1)).finishTestItem(eq(id), any(FinishTestItemRQ.class));
	}

	@Test
	public void finishTestMethodSkipped() {
		when(launch.getParameters()).thenReturn(new ListenerParameters());
		when(launch.startTestItem(any(Maybe.class), any())).thenReturn(id);
		when(launch.finishTestItem(any(Maybe.class), any())).thenReturn(complete);
		when(launch.getStepReporter()).thenReturn(stepReporter);
		when(testResult.getTestContext()).thenReturn(testContext);
		when(testResult.getMethod()).thenReturn(method);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setSkippedAnIssue(true);
		when(launch.getParameters()).thenReturn(listenerParameters);
		when(testResult.getAttribute(RP_ID)).thenReturn(null).thenReturn(id);
		when(method.isTest()).thenReturn(true);

		testNGService.finishTestMethod(Statuses.SKIPPED, testResult);

		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testResult, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
		verify(launch, times(1)).finishTestItem(any(Maybe.class), any(FinishTestItemRQ.class));
	}

	@Test
	public void startConfiguration() {
		when(launch.startTestItem(any(Maybe.class), any())).thenReturn(id);
		when(testResult.getTestContext()).thenReturn(testContext);
		when(testResult.getMethod()).thenReturn(method);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);

		testNGService.startConfiguration(testResult);
		verify(launch, times(1)).startTestItem(any(Maybe.class), any(StartTestItemRQ.class));
		verify(testResult, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

	@Test
	public void testParentConfigSuite() {
		when(testResult.getTestContext()).thenReturn(testContext);
		when(testContext.getSuite()).thenReturn(suite);
		when(suite.getAttribute(RP_ID)).thenReturn(id);

		Maybe<String> configParent = testNGService.getConfigParent(testResult, TestMethodType.BEFORE_SUITE);
		assertThat("Incorrect id", configParent, is(id));
	}

	@Test
	public void testParentConfig() {
		when(testResult.getTestContext()).thenReturn(testContext);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);

		Maybe<String> configParent = testNGService.getConfigParent(testResult, TestMethodType.STEP);
		assertThat("Incorrect id", configParent, is(id));
	}

}
