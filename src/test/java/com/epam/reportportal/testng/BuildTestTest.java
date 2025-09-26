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

import java.time.Instant;
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
		assertThat("Incorrect launch tags", startLaunchRQ.getAttributes(), hasItem(ATTRIBUTE));
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
        assertThat("Incorrect suite start time", rq.getStartTime(), is(instance.getTime().toInstant()));
	}

	@Test
	public void testFinishTestRqPassed() {
        Date endTime = new Date(DEFAULT_TIME);
        when(testContext.getEndDate()).thenReturn(endTime);

		FinishTestItemRQ rq = testNGService.buildFinishTestRq(testContext);
        assertThat("Incorrect end time", rq.getEndTime(), is(endTime.toInstant()));
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
