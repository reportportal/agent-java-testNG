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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.Assert;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
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
	private ITestContext testContext;

	@Mock
	private ITestResult testResult;

	@Mock
	private ITestNGMethod method;

	@Mock
	private Launch launch;

	@Mock
	private ISuite suite;

	@Mock
	private Maybe<String> id;

	@BeforeClass
	public void init() {
		MockitoAnnotations.initMocks(this);
		testNGService = new TestNGService(new TestNGService.MemoizingSupplier<Launch>(new Supplier<Launch>() {
			@Override
			public Launch get() {
				return launch;
			}
		}));
	}

	@BeforeMethod
	public void preconditions() {
		when(testResult.getTestContext()).thenReturn(testContext);
		when(testResult.getMethod()).thenReturn(method);
		when(testResult.getAttribute(RP_ID)).thenReturn(id);
		when(testContext.getSuite()).thenReturn(suite);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);
	}

	@AfterMethod
	public void after() {
		reset(launch, testResult);
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
		when(suite.getAttribute(RP_ID)).thenReturn(id);
		testNGService.startTest(testContext);

		verify(launch, times(1)).startTestItem(eq(id), any(StartTestItemRQ.class));
		verify(testContext, times(1)).setAttribute(eq(RP_ID), Matchers.any(Maybe.class));
	}

	@Test
	public void finishTest() {
		ResultMap empty = new ResultMap();
		when(testContext.getFailedTests()).thenReturn(empty);
		when(testContext.getFailedConfigurations()).thenReturn(empty);
		when(testContext.getSkippedConfigurations()).thenReturn(empty);
		when(testContext.getSkippedTests()).thenReturn(empty);

		testNGService.finishTest(testContext);
		verify(launch, times(1)).finishTestItem(eq(id), any(FinishTestItemRQ.class));
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
