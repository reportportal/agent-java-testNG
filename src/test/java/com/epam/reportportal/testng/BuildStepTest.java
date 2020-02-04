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

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.IRetryAnalyzer;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.internal.ConstructorOrMethod;
import rp.com.google.common.base.Supplier;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import static com.epam.reportportal.testng.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

/**
 * @author Pavel Bortnik
 */
public class BuildStepTest {

	private TestNGService testNGService;

	@Mock
	private Launch launch;

	@Mock
	private ITestResult testResult;

	@Mock
	private ITestNGMethod testNGMethod;

	@Mock
	private ConstructorOrMethod constructorOrMethod;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
		testNGService = new TestNGService(new TestNGService.MemoizingSupplier<Launch>(new Supplier<Launch>() {
			@Override
			public Launch get() {
				return launch;
			}
		}));

		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		when(testNGMethod.getRetryAnalyzer(testResult)).thenReturn(new IRetryAnalyzer()
		{
			@Override
			public boolean retry(ITestResult result)
			{
				return false;
			}
		});
	}

	@Test
	public void testTestName() {
		when(testResult.getTestName()).thenReturn(DEFAULT_NAME);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item name", rq.getName(), is(DEFAULT_NAME));
	}

	@Test
	public void testMethodName() {
		when(testNGMethod.getMethodName()).thenReturn(DEFAULT_NAME);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item name", rq.getName(), is(DEFAULT_NAME));
	}

	@Test
	public void testDescription() {
		when(testNGMethod.getDescription()).thenReturn(DEFAULT_DESCRIPTION);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test description", rq.getDescription(), is(DEFAULT_DESCRIPTION));
	}

	@Test
	public void testParameters() {
		Method[] methods = TestMethodsExamples.class.getDeclaredMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().contains("parametersAnnotation")) {
				method = m;
			}
		}
		when(constructorOrMethod.getMethod()).thenReturn(method);
		when(testResult.getParameters()).thenReturn(new Object[] { "param_0", "param_1" });
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);

		Assert.assertThat("Incorrect parameter key", rq.getParameters().get(0).getKey(), is("message_0"));
		Assert.assertThat("Incorrect parameter value", rq.getParameters().get(0).getValue(), is("param_0"));

		Assert.assertThat("Incorrect parameter key", rq.getParameters().get(1).getKey(), is("message_1"));
		Assert.assertThat("Incorrect parameter value", rq.getParameters().get(1).getValue(), is("param_1"));

	}

	@Test
	public void testParametersDataProvider_DefaultKey() {
		Method[] methods = TestMethodsExamples.class.getDeclaredMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().contains("dataProviderWithoutKey")) {
				method = m;
			}
		}

		when(constructorOrMethod.getMethod()).thenReturn(method);
		when(testResult.getParameters()).thenReturn(new Object[] { "param_0", "param_1" });
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);

		Assert.assertThat("Incorrect parameter key", rq.getParameters().get(0).getKey(), is("arg0"));
		Assert.assertThat("Incorrect parameter value", rq.getParameters().get(0).getValue(), is("param_0"));

		Assert.assertThat("Incorrect parameter key", rq.getParameters().get(1).getKey(), is("arg1"));
		Assert.assertThat("Incorrect parameter value", rq.getParameters().get(1).getValue(), is("param_1"));

	}

	@Test
	public void testParametersDataProvider_ProvidedKey() {
		Method[] methods = TestMethodsExamples.class.getDeclaredMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().contains("dataProviderWithParameterKey")) {
				method = m;
			}
		}

		when(constructorOrMethod.getMethod()).thenReturn(method);
		when(testResult.getParameters()).thenReturn(new Object[] { "param_0", "param_1" });
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		Assert.assertThat("Incorrect parameter key", rq.getParameters().get(0).getKey(), is("key_0"));
		Assert.assertThat("Incorrect parameter value", rq.getParameters().get(0).getValue(), is("param_0"));

		Assert.assertThat("Incorrect parameter key", rq.getParameters().get(1).getKey(), is("arg1"));
		Assert.assertThat("Incorrect parameter value", rq.getParameters().get(1).getValue(), is("param_1"));

	}

	@Test
	public void testParametersNotSpecified() {
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item parameters", rq.getParameters(), nullValue());
	}

	@Test
	public void testUniqueId() {
		Method[] methods = TestMethodsExamples.class.getDeclaredMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().contains("uniqueIdAnnotation")) {
				method = m;
			}
		}

		when(constructorOrMethod.getMethod()).thenReturn(method);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		Assert.assertThat("Incorrect unique id", rq.getUniqueId(), is("ProvidedID"));

	}

	@Test
	public void testUniqueIdNotSpecified() {
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item unique id", rq.getUniqueId(), nullValue());
	}

	@Test
	public void testStartTime() {
		Calendar instance = Calendar.getInstance();
		instance.setTimeInMillis(DEFAULT_TIME);
		when(testResult.getStartMillis()).thenReturn(instance.getTimeInMillis());
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect start time", rq.getStartTime(), is(instance.getTime()));
	}

	@Test
	public void testType() {
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item type", rq.getType(), is("STEP"));
	}

	@Test
	public void testRetryFlagPositive() {
		when(testNGMethod.getRetryAnalyzer(any(ITestResult.class))).thenReturn(new IRetryAnalyzer()
		{
			@Override
			public boolean retry(ITestResult result)
			{
				return true;
			}
		});
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect retry flag", rq.isRetry(), is(true));
	}

	@Test
	public void testRetryFlagNegative() {
		when(testNGMethod.getCurrentInvocationCount()).thenReturn(0);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect retry flag", rq.isRetry(), is(false));
	}

	@Test
	public void testRetryAnalyzerNull() {
		when(testNGMethod.getRetryAnalyzer(any(ITestResult.class))).thenReturn(null);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect retry flag", rq.isRetry(), is(false));
	}

	@Test
	public void testBuildFinishRQ() {
		when(testResult.getEndMillis()).thenReturn(DEFAULT_TIME);
		FinishTestItemRQ rq = testNGService.buildFinishTestMethodRq(Statuses.PASSED, testResult);
		assertThat("Incorrect end time", rq.getEndTime().getTime(), is(DEFAULT_TIME));
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.PASSED));
		assertThat("Incorrect issue", rq.getIssue(), nullValue());
	}

	@Test
	public void testSkippedNotIssue() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setSkippedAnIssue(false);
		when(launch.getParameters()).thenReturn(listenerParameters);

		FinishTestItemRQ rq = testNGService.buildFinishTestMethodRq(Statuses.SKIPPED, testResult);
		assertThat("Incorrect issue type", rq.getIssue().getIssueType(), is("NOT_ISSUE"));
	}

	@Test
	public void testStartConfigurationRq() {
		when(testNGMethod.getMethodName()).thenReturn(DEFAULT_NAME);
		when(testNGMethod.getDescription()).thenReturn(DEFAULT_DESCRIPTION);
		when(testResult.getStartMillis()).thenReturn(DEFAULT_TIME);

		StartTestItemRQ rq = testNGService.buildStartConfigurationRq(testResult, TestMethodType.BEFORE_TEST);

		assertThat("Incorrect method name", rq.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect method description", rq.getDescription(), is(DEFAULT_DESCRIPTION));
		assertThat("Incorrect start time", rq.getStartTime(), is(new Date(DEFAULT_TIME)));
		assertThat("Incorrect test method type", rq.getType(), is(TestMethodType.BEFORE_TEST.name()));
	}

	@Test
	public void testStartConfigurationNullType() {
		StartTestItemRQ rq = testNGService.buildStartConfigurationRq(testResult, null);
		assertThat("Incorrect method type", rq.getType(), nullValue());
	}

	private static class TestMethodsExamples {
		@UniqueID("ProvidedID")
		@org.testng.annotations.Test
		private void uniqueIdAnnotation() {
			//just for testing providing unique id
		}

		@Ignore
		@org.testng.annotations.Test(dataProvider = "dp")
		private void dataProviderWithoutKey(String param_0, String param_1) {
			//just for testing providing parameters
		}

		@Ignore
		@org.testng.annotations.Test(dataProvider = "dp")
		private void dataProviderWithParameterKey(@ParameterKey("key_0") String param_0, String param_1) {
			//just for testing providing parameters
		}

		@org.testng.annotations.Test
		@Parameters({ "message_0", "message_1" })
		private void parametersAnnotation(String msg_0, String msq_1) {
			//just for testing providing parameters
		}

		@DataProvider(name = "dp")
		private Iterator<Object[]> params() {
			return Arrays.asList(new Object[] { "one", "two" }, new Object[] { "two", "one" }).iterator();
		}
	}

}
