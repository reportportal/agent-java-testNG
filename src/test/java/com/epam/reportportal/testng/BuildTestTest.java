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
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.hamcrest.Matchers;
import com.epam.reportportal.utils.MemoizingSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testng.*;
import org.testng.internal.ResultMap;

import java.util.*;
import java.util.regex.Pattern;

import static com.epam.reportportal.testng.Constants.*;
import static com.epam.reportportal.testng.TestNGService.SKIPPED_ISSUE_KEY;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Pavel Bortnik
 */
public class BuildTestTest {

	private static final Map<String, Pattern> predefinedProperties = new HashMap<>();

	@BeforeAll
	public static void initKeys() {
		predefinedProperties.put("os", Pattern.compile("^.+\\|.+\\|.+$"));
		predefinedProperties.put("jvm", Pattern.compile("^.+\\|.+\\|.+$"));
		predefinedProperties.put("agent", Pattern.compile("^agent-java-testng\\|.+$"));
	}

	private TestNGService testNGService;

	@Mock
	private ITestContext testContext;

	@Mock
	private ISuite iSuite;

	@Mock
	private Launch launch;

	@BeforeEach
	public void preconditions() {
		testNGService = new TestNGService(new MemoizingSupplier<>(() -> launch));
	}

	@Test
	public void testStartLaunchRq() {
		ListenerParameters listenerParameters = defaultListenerParameters();
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(listenerParameters);
		assertThat("Incorrect launch name", startLaunchRQ.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect start time", startLaunchRQ.getStartTime(), notNullValue());
		assertThat("Incorrect launch tags", startLaunchRQ.getAttributes(), is(ATTRIBUTES));
		assertThat("Incorrect launch mode", startLaunchRQ.getMode(), is(MODE));
		assertThat("Incorrect description", startLaunchRQ.getDescription(), is(DEFAULT_DESCRIPTION));
	}

	@Test
	public void testSkippedIssue() {

		ItemAttributesRQ itemAttributeResource = new ItemAttributesRQ();
		itemAttributeResource.setKey(SKIPPED_ISSUE_KEY);
		itemAttributeResource.setValue(String.valueOf(true));
		itemAttributeResource.setSystem(true);

		ListenerParameters parameters = new ListenerParameters();
		parameters.setSkippedAnIssue(true);
		parameters.setAttributes(new HashSet<>());
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(parameters);
		assertTrue(startLaunchRQ.getAttributes().contains(itemAttributeResource));
	}

	@Test
	public void testPredefinedAttributes() {
		final ListenerParameters parameters = new ListenerParameters();
		parameters.setSkippedAnIssue(null);
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(parameters);
		assertThat(startLaunchRQ.getAttributes().size(), Matchers.is(3));
		Set<String> keys = startLaunchRQ.getAttributes().stream().map(ItemAttributeResource::getKey).collect(toSet());
		predefinedProperties.forEach((predefinedKey, predefinedValue) -> assertTrue(keys.contains(predefinedKey)));
		startLaunchRQ.getAttributes().forEach(attribute -> {
			assertThat(attribute.getValue(), matchesPattern(predefinedProperties.get(attribute.getKey())));
			assertTrue(attribute.isSystem());
		});
	}

	@Test
	public void testStartLaunchRq_EmptyDescription() {
		ListenerParameters parameters = new ListenerParameters();
		parameters.setDescription("");
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(parameters);
		assertThat("Description should be null", startLaunchRQ.getDescription(), nullValue());
	}

	@Test
	public void testStartLaunchRq_NullDescription() {
		ListenerParameters parameters = new ListenerParameters();
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(parameters);
		assertThat("Description should be null", startLaunchRQ.getDescription(), nullValue());
	}

	@Test
	public void testStartSuiteRq() {
		ISuite suite = mock(ISuite.class);
		when(suite.getName()).thenReturn(DEFAULT_NAME);

		StartTestItemRQ rq = testNGService.buildStartSuiteRq(suite);

		assertThat("Incorrect suite item type", rq.getType(), is("SUITE"));
		assertThat("Incorrect suite name", rq.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect suite start time", rq.getStartTime(), notNullValue());
	}

	@Test
	public void testStartTestRq() {
		ITestContext testContext = mock(ITestContext.class);
		Calendar instance = Calendar.getInstance();
		instance.setTimeInMillis(DEFAULT_TIME);

		when(testContext.getName()).thenReturn(DEFAULT_NAME);
		when(testContext.getStartDate()).thenReturn(instance.getTime());

		StartTestItemRQ rq = testNGService.buildStartTestItemRq(testContext);
		assertThat("Incorrect test item type", rq.getType(), is("TEST"));
		assertThat("Incorrect test item name", rq.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect suite start time", rq.getStartTime(), is(instance.getTime()));
	}

	@Test
	public void testFinishTestRqPassed() {
		ResultMap empty = new ResultMap();
		Date endTime = new Date(DEFAULT_TIME);
		when(testContext.getEndDate()).thenReturn(endTime);
		when(testContext.getFailedTests()).thenReturn(empty);
		when(testContext.getFailedConfigurations()).thenReturn(empty);
		when(testContext.getSkippedTests()).thenReturn(empty);
		when(testContext.getSkippedConfigurations()).thenReturn(empty);

		FinishTestItemRQ rq = testNGService.buildFinishTestRq(testContext);
		assertThat("Incorrect end time", rq.getEndTime(), is(endTime));
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.PASSED));
	}

	@Test
	public void testFinishHasFailedTests() {
		IResultMap failedTests = mock(IResultMap.class);

		ResultMap empty = new ResultMap();

		when(failedTests.size()).thenReturn(1);
		when(testContext.getFailedTests()).thenReturn(failedTests);

		FinishTestItemRQ rq = testNGService.buildFinishTestRq(testContext);
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.FAILED));
	}

	@Test
	public void testFinishHasFailedConfigurations() {
		IResultMap failedConfigurations = mock(IResultMap.class);

		ResultMap empty = new ResultMap();

		when(testContext.getFailedTests()).thenReturn(empty);

		when(failedConfigurations.size()).thenReturn(1);
		when(testContext.getFailedConfigurations()).thenReturn(failedConfigurations);

		FinishTestItemRQ rq = testNGService.buildFinishTestRq(testContext);
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.FAILED));
	}

	@Test
	public void testFinishHasSkippedTest() {
		IResultMap skippedTests = mock(IResultMap.class);

		ResultMap empty = new ResultMap();

		when(testContext.getFailedTests()).thenReturn(empty);
		when(testContext.getFailedConfigurations()).thenReturn(empty);
		when(testContext.getSkippedConfigurations()).thenReturn(empty);

		when(skippedTests.size()).thenReturn(1);
		when(skippedTests.getAllResults()).thenReturn(Collections.singleton(mock(ITestResult.class)));
		when(testContext.getSkippedTests()).thenReturn(skippedTests);

		FinishTestItemRQ rq = testNGService.buildFinishTestRq(testContext);
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.FAILED));
	}

	@Test
	public void testFinishHasSkippedConfigurationsTest() {
		IResultMap skippedConfigurations = mock(IResultMap.class);

		ResultMap empty = new ResultMap();

		when(testContext.getFailedTests()).thenReturn(empty);
		when(testContext.getFailedConfigurations()).thenReturn(empty);

		when(skippedConfigurations.size()).thenReturn(1);
		when(testContext.getSkippedConfigurations()).thenReturn(skippedConfigurations);

		FinishTestItemRQ rq = testNGService.buildFinishTestRq(testContext);
		assertThat("Incorrect status", rq.getStatus(), is(Statuses.FAILED));
	}

	@Test
	public void testFinishSuite() {
		FinishTestItemRQ rq = testNGService.buildFinishTestSuiteRq(iSuite);
		assertThat(rq.getEndTime(), notNullValue());
		assertThat(rq.getStatus(), is(Statuses.PASSED));
	}

	@Test
	public void testFinishSuiteFailed() {
		ISuiteResult suiteResult = mock(ISuiteResult.class);
		Map<String, ISuiteResult> suiteResults = new HashMap<String, ISuiteResult>(1);
		suiteResults.put("", suiteResult);
		IResultMap resultMap = mock(ResultMap.class);

		when(iSuite.getResults()).thenReturn(suiteResults);
		when(suiteResult.getTestContext()).thenReturn(testContext);
		when(testContext.getFailedTests()).thenReturn(resultMap);
		when(resultMap.size()).thenReturn(1);

		FinishTestItemRQ rq = testNGService.buildFinishTestSuiteRq(iSuite);
		assertThat(rq.getStatus(), is(Statuses.FAILED));
	}

	private ListenerParameters defaultListenerParameters() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl(BASIC_URL);
		listenerParameters.setApiKey(DEFAULT_UUID);
		listenerParameters.setLaunchName(DEFAULT_NAME);
		listenerParameters.setProjectName(DEFAULT_PROJECT);
		listenerParameters.setAttributes(ATTRIBUTES);
		listenerParameters.setLaunchRunningMode(MODE);
		listenerParameters.setDescription(DEFAULT_DESCRIPTION);
		return listenerParameters;
	}

}
