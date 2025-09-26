/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.testng.integration.feature.issue.ParameterizedWithOneIssueTest;
import com.epam.reportportal.testng.integration.feature.issue.ParameterizedWithTwoIssueTest;
import com.epam.reportportal.testng.integration.feature.issue.SimpleIssueTest;
import com.epam.reportportal.testng.integration.feature.issue.SimpleTwoIssuesTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssueReportingTest {
	public static class TestReportPortalListener extends BaseTestNGListener {
		public static final ThreadLocal<Launch> LAUNCH_THREAD_LOCAL = new ThreadLocal<>();

		public TestReportPortalListener() {
			super(new TestNGService(LAUNCH_THREAD_LOCAL::get));
		}

		public static void initLaunch(Launch launch) {
			LAUNCH_THREAD_LOCAL.set(launch);
		}

		public static Launch getLaunch() {
			return LAUNCH_THREAD_LOCAL.get();
		}
	}

	private final String suiteId = CommonUtils.namedId("suite_");
	private final Maybe<String> suiteMaybe = Maybe.just(suiteId);
	private final String stepOneId = CommonUtils.namedId("step_");
	private final Maybe<String> stepOneMaybe = Maybe.just(stepOneId);
	private final String stepTwoId = CommonUtils.namedId("step_");
	private final Maybe<String> stepTwoMaybe = Maybe.just(stepTwoId);
	private final String stepThreeId = CommonUtils.namedId("step_");
	private final Maybe<String> stepThreeMaybe = Maybe.just(stepThreeId);
	private final Queue<Maybe<String>> stepIds = new LinkedList<>(Arrays.asList(stepOneMaybe, stepTwoMaybe, stepThreeMaybe));

	@Mock
	private Launch launch;
	@Mock
	private ListenerParameters parameters;

	@BeforeEach
	public void initMocks() {
		when(launch.getParameters()).thenReturn(parameters);
		when(launch.startTestItem(any())).thenReturn(suiteMaybe);
		when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(launch.startTestItem(same(suiteMaybe), any())).thenAnswer((Answer<Maybe<String>>) invocation -> stepIds.poll());
		when(launch.startTestItem(same(stepOneMaybe), any())).thenAnswer((Answer<Maybe<String>>) invocation -> stepIds.poll());
		when(launch.finishTestItem(
				any(),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> Maybe.just(new OperationCompletionRS("OK")));
		TestReportPortalListener.initLaunch(launch);
		when(parameters.isCallbackReportingEnabled()).thenReturn(Boolean.TRUE);
	}

	@Test
	public void verify_simple_test_failure() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), SimpleIssueTest.class);

		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(SimpleIssueTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_test_failure_with_two_issues() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), SimpleTwoIssuesTest.class);

		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(SimpleTwoIssuesTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_parameterized_test_failure_with_one_issue() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), ParameterizedWithOneIssueTest.class);

		ArgumentCaptor<FinishTestItemRQ> firstTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), firstTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> secondTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepThreeMaybe), secondTestCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = firstTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), nullValue());

		finishTestItemRQ = secondTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(ParameterizedWithOneIssueTest.ISSUE_MESSAGE));
	}

	@Test
	public void verify_parameterized_test_failure_with_two_issues() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), ParameterizedWithTwoIssueTest.class);

		ArgumentCaptor<FinishTestItemRQ> firstTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), firstTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> secondTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepThreeMaybe), secondTestCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = firstTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(ParameterizedWithTwoIssueTest.ISSUE_MESSAGE));

		finishTestItemRQ = secondTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(ParameterizedWithTwoIssueTest.ISSUE_MESSAGE));
	}
}
