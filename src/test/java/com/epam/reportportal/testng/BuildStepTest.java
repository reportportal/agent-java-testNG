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

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.TestCaseIdKey;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Parameters;
import org.testng.internal.ConstructorOrMethod;

import java.lang.reflect.Method;
import java.util.*;

import static com.epam.reportportal.testng.Constants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

	@BeforeEach
	public void initMocks() {
		testNGService = new TestNGService(new MemoizingSupplier<>(() -> launch));
	}

	@Test
	public void testMethodName() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		when(testNGMethod.getMethodName()).thenReturn(DEFAULT_NAME);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item name", rq.getName(), is(DEFAULT_NAME));
	}

	@Test
	public void testDescription() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		when(testNGMethod.getDescription()).thenReturn(DEFAULT_DESCRIPTION);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test description", rq.getDescription(), is(DEFAULT_DESCRIPTION));
	}

	@Test
	public void testParameters() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
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

		assertThat("Incorrect parameter key", rq.getParameters().get(0).getKey(), is("message_0"));
		assertThat("Incorrect parameter value", rq.getParameters().get(0).getValue(), is("param_0"));

		assertThat("Incorrect parameter key", rq.getParameters().get(1).getKey(), is("message_1"));
		assertThat("Incorrect parameter value", rq.getParameters().get(1).getValue(), is("param_1"));

	}

	@Test
	public void testParametersDataProvider_DefaultKey() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
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

		assertThat("Incorrect parameter key", rq.getParameters().get(0).getKey(), is(String.class.getCanonicalName()));
		assertThat("Incorrect parameter value", rq.getParameters().get(0).getValue(), is("param_0"));

		assertThat("Incorrect parameter key", rq.getParameters().get(1).getKey(), is(String.class.getCanonicalName()));
		assertThat("Incorrect parameter value", rq.getParameters().get(1).getValue(), is("param_1"));
	}

	@Test
	public void testParametersDataProvider_ProvidedKey() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
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
		assertThat("Incorrect parameter key", rq.getParameters().get(0).getKey(), is("key_0"));
		assertThat("Incorrect parameter value", rq.getParameters().get(0).getValue(), is("param_0"));

		assertThat("Incorrect parameter key", rq.getParameters().get(1).getKey(), is(String.class.getCanonicalName()));
		assertThat("Incorrect parameter value", rq.getParameters().get(1).getValue(), is("param_1"));

	}

	@Test
	public void testParametersNotSpecified() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item parameters", rq.getParameters(), nullValue());
	}

	@Test
	public void testUniqueId() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		Method[] methods = TestMethodsExamples.class.getDeclaredMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().contains("uniqueIdAnnotation")) {
				method = m;
			}
		}

		when(constructorOrMethod.getMethod()).thenReturn(method);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect unique id", rq.getUniqueId(), is("ProvidedID"));

	}

	@Test
	public void testUniqueIdNotSpecified() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item unique id", rq.getUniqueId(), nullValue());
	}

	@Test
	public void testStartTime() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		Calendar instance = Calendar.getInstance();
		instance.setTimeInMillis(DEFAULT_TIME);
		when(testResult.getStartMillis()).thenReturn(instance.getTimeInMillis());
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect start time", rq.getStartTime(), is(instance.getTime()));
	}

	@Test
	public void testType() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect test item type", rq.getType(), is("STEP"));
	}

	@Test
	public void testRetryAnalyzerNull() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		StartTestItemRQ rq = testNGService.buildStartStepRq(testResult);
		assertThat("Incorrect retry flag", rq.isRetry(), nullValue());
	}

	@Test
	public void testBuildFinishRQ() {
		when(testResult.getEndMillis()).thenReturn(DEFAULT_TIME);
		FinishTestItemRQ rq = testNGService.buildFinishTestMethodRq(ItemStatus.PASSED, testResult);
		assertThat("Incorrect end time", rq.getEndTime().getTime(), is(DEFAULT_TIME));
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.PASSED));
		assertThat("Incorrect issue", rq.getIssue(), nullValue());
	}

	@Test
	public void testStartConfigurationRq() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
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
		when(testResult.getMethod()).thenReturn(testNGMethod);
		StartTestItemRQ rq = testNGService.buildStartConfigurationRq(testResult, null);
		assertThat("Incorrect method type", rq.getType(), nullValue());
	}

	@Test
	public void codeRefTest() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		String expected = "com.test.BuildStepTest.codeRefTest";
		when(testResult.getMethod().getQualifiedName()).thenReturn(expected);

		StartTestItemRQ request = testNGService.buildStartStepRq(testResult);

		assertEquals(expected, request.getCodeRef());
	}

	@Test
	public void testCaseId_fromCodeRef() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		String expected = "com.test.BuildStepTest.codeRefTest";
		when(testResult.getMethod().getQualifiedName()).thenReturn(expected);

		StartTestItemRQ request = testNGService.buildStartStepRq(testResult);

		assertEquals(expected, request.getCodeRef());
		assertEquals(expected, request.getTestCaseId());
	}

	@Test
	public void testCaseId_fromCodeRefAndParams() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		String expectedCodeRef = "com.test.BuildStepTest.codeRefTest";
		String expectedParam1 = "param_0";
		String expectedParam2 = "param_1";

		when(testResult.getMethod().getQualifiedName()).thenReturn(expectedCodeRef);
		when(testResult.getParameters()).thenReturn(new Object[] { expectedParam1, expectedParam2 });

		StartTestItemRQ request = testNGService.buildStartStepRq(testResult);

		assertEquals(expectedCodeRef, request.getCodeRef());
		assertEquals(expectedCodeRef + "[" + expectedParam1 + "," + expectedParam2 + "]", request.getTestCaseId());
	}

	@Test
	public void testCaseId_fromAnnotation() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		Optional<Method> methodOptional = Arrays.stream(TestMethodsExamples.class.getDeclaredMethods())
				.filter(it -> it.getName().equals("testCaseId"))
				.findFirst();
		assertTrue(methodOptional.isPresent());
		String expectedCodeRef = "com.test.BuildStepTest.codeRefTest";

		when(constructorOrMethod.getMethod()).thenReturn(methodOptional.get());
		when(testResult.getMethod().getQualifiedName()).thenReturn(expectedCodeRef);

		StartTestItemRQ request = testNGService.buildStartStepRq(testResult);

		assertEquals(expectedCodeRef, request.getCodeRef());
		assertEquals("test-case-id", request.getTestCaseId());
	}

	@Test
	public void testCaseId_fromAnnotationParametrized() {
		when(testResult.getMethod()).thenReturn(testNGMethod);
		when(testNGMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);
		when(testNGMethod.isTest()).thenReturn(true);
		Optional<Method> methodOptional = Arrays.stream(TestMethodsExamples.class.getDeclaredMethods())
				.filter(it -> it.getName().equals("testCaseIdParameterized"))
				.findFirst();
		assertTrue(methodOptional.isPresent());
		String expectedCodeRef = "com.test.BuildStepTest.codeRefTest";
		String expectedParam = "test-case-id-key";

		when(constructorOrMethod.getMethod()).thenReturn(methodOptional.get());
		when(testResult.getMethod().getQualifiedName()).thenReturn(expectedCodeRef);
		when(testResult.getParameters()).thenReturn(new Object[] { expectedParam });

		StartTestItemRQ request = testNGService.buildStartStepRq(testResult);

		assertEquals(expectedCodeRef, request.getCodeRef());
		assertEquals(expectedParam, request.getTestCaseId());
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

		@TestCaseId("test-case-id")
		private void testCaseId() {
			//just for testing providing annotation
		}

		@TestCaseId(parametrized = true)
		private void testCaseIdParameterized(@TestCaseIdKey String param) {
			//just for testing providing annotation
		}
	}

}
