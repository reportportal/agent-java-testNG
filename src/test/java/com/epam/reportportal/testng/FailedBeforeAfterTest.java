/*
 * Copyright 2021 EPAM Systems
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

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.LaunchImpl;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.testng.integration.feature.skipped.*;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FailedBeforeAfterTest {
	public static class SkippedTestExtension extends BaseTestNGListener {
		static Launch LAUNCH;

		public SkippedTestExtension() {
			super(new TestNGService(() -> LAUNCH));
		}

		public static void initLaunch(Launch launch) {
			LAUNCH = launch;
		}
	}

	private final Maybe<String> suitedUuid = createMaybe(namedUuid("suite"));
	private final Maybe<String> testClassUuid = createMaybe(namedUuid("class"));
	private final List<Maybe<String>> testMethodUuidList = Arrays.asList(createMaybe(namedUuid("before")),
			createMaybe(namedUuid("test")),
			createMaybe(namedUuid("after"))
	);
	private final List<String> finishUuidOrder = Stream.concat(testMethodUuidList.stream().map(Maybe::blockingGet),
			Stream.of(testClassUuid.blockingGet(), suitedUuid.blockingGet())
	)
			.collect(Collectors.toList());

	@Mock
	private Launch launch;
	@Mock
	private StepReporter reporter;

	@BeforeEach
	public void setupMock() {
		mockLaunch(launch,
				reporter,
				createMaybe("launchUuid"),
				suitedUuid,
				testClassUuid,
				Stream.concat(testMethodUuidList.stream(), Stream.concat(testMethodUuidList.stream(), testMethodUuidList.stream()))
						.collect(Collectors.toList())
		);
		SkippedTestExtension.initLaunch(launch);
	}

	@ParameterizedTest
	@ValueSource(classes = { RetryFailedBeforeTest.class, BeforeMethodFailedTest.class })
	@SuppressWarnings("unchecked")
	public void agent_should_report_skipped_test_in_case_of_failed_before_method(Class<?> test) {
		TestUtils.runTests(Collections.singletonList(SkippedTestExtension.class), test);

		verify(launch, times(1)).start(); // Start launch
		verify(launch, times(1)).startTestItem(any());  // Start parent suites
		verify(launch, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startItemCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		// 1 before method, 1 test, 1 after method
		verify(launch, times(3)).startTestItem(same(testClassUuid), startItemCapture.capture());

		List<StartTestItemRQ> startItems = startItemCapture.getAllValues();
		assertThat(startItems.get(0).getType(), equalTo(TestMethodType.BEFORE_METHOD.name()));
		assertThat(startItems.get(1).getType(), equalTo(TestMethodType.STEP.name()));
		assertThat(startItems.get(2).getType(), equalTo(TestMethodType.AFTER_METHOD.name()));

		ArgumentCaptor<Maybe<String>> finishUuidCapture = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		// 1 before method, 1 test, 1 after method, finish test class, finish suite = 5
		verify(launch, times(5)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

		List<String> finishUuids = finishUuidCapture.getAllValues().stream().map(Maybe::blockingGet).collect(Collectors.toList());
		assertThat(finishUuids, equalTo(finishUuidOrder));

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
		assertThat(finishItems.get(0).getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishItems.get(0).getIssue(), nullValue());

		finishItems.subList(1, 3).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
			assertThat(e.getIssue(), allOf(notNullValue(), sameInstance(Launch.NOT_ISSUE)));
		});

		finishItems.subList(3, finishItems.size()).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.FAILED.name()));
			assertThat(e.getIssue(), nullValue());
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void agent_should_report_skipped_test_in_case_of_failed_before_class() {
		TestUtils.runTests(Collections.singletonList(SkippedTestExtension.class), BeforeClassFailedTest.class);

		verify(launch, times(1)).start(); // Start launch
		verify(launch, times(1)).startTestItem(any());  // Start parent suites
		verify(launch, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startItemCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		// 1 before method, 1 test, 1 after method
		verify(launch, times(3)).startTestItem(same(testClassUuid), startItemCapture.capture());

		List<StartTestItemRQ> startItems = startItemCapture.getAllValues();
		assertThat(startItems.get(0).getType(), equalTo(TestMethodType.BEFORE_CLASS.name()));
		assertThat(startItems.get(1).getType(), equalTo(TestMethodType.STEP.name()));
		assertThat(startItems.get(2).getType(), equalTo(TestMethodType.AFTER_CLASS.name()));

		ArgumentCaptor<Maybe<String>> finishUuidCapture = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		// 1 before method, 1 test, 1 after method, finish test class, finish suite = 5
		verify(launch, times(5)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

		List<String> finishUuids = finishUuidCapture.getAllValues().stream().map(Maybe::blockingGet).collect(Collectors.toList());
		assertThat(finishUuids, equalTo(finishUuidOrder));

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
		assertThat(finishItems.get(0).getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishItems.get(0).getIssue(), nullValue());

		finishItems.subList(1, 3).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
			assertThat(e.getIssue(), allOf(notNullValue(), sameInstance(Launch.NOT_ISSUE)));
		});

		finishItems.subList(3, finishItems.size()).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.FAILED.name()));
			assertThat(e.getIssue(), nullValue());
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void agent_should_report_skipped_parametrized_tests_in_case_of_failed_before_method() {
		TestUtils.runTests(Collections.singletonList(SkippedTestExtension.class), BeforeMethodFailedParametrizedTest.class);

		verify(launch, times(1)).start(); // Start launch
		verify(launch, times(1)).startTestItem(any());  // Start parent suites
		verify(launch, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startItemCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		// 1 before method, 1 test, 1 after method x 2 = 6, since data providers create separate tests
		verify(launch, times(6)).startTestItem(same(testClassUuid), startItemCapture.capture());

		List<StartTestItemRQ> startItems = startItemCapture.getAllValues();
		assertThat(startItems.get(0).getType(), equalTo(TestMethodType.BEFORE_METHOD.name()));
		assertThat(startItems.get(1).getType(), equalTo(TestMethodType.STEP.name()));
		assertThat(startItems.get(2).getType(), equalTo(TestMethodType.AFTER_METHOD.name()));
		assertThat(startItems.get(3).getType(), equalTo(TestMethodType.BEFORE_METHOD.name()));
		assertThat(startItems.get(4).getType(), equalTo(TestMethodType.STEP.name()));
		assertThat(startItems.get(5).getType(), equalTo(TestMethodType.AFTER_METHOD.name()));

		ArgumentCaptor<Maybe<String>> finishUuidCapture = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		// 1 before method, 1 test, 1 after method x 2, finish test class, finish suite = 8
		verify(launch, times(8)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

		List<String> finishUuids = finishUuidCapture.getAllValues().stream().map(Maybe::blockingGet).collect(Collectors.toList());
		assertThat(finishUuids,
				equalTo(Stream.concat(testMethodUuidList.stream().map(Maybe::blockingGet), finishUuidOrder.stream())
						.collect(Collectors.toList()))
		);

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
		assertThat(finishItems.get(0).getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishItems.get(0).getIssue(), nullValue());

		finishItems.subList(1, 6).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
			assertThat(e.getIssue(), allOf(notNullValue(), sameInstance(Launch.NOT_ISSUE)));
		});

		finishItems.subList(6, finishItems.size()).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.FAILED.name()));
			assertThat(e.getIssue(), nullValue());
		});
	}

	@ParameterizedTest
	@ValueSource(classes = { RetryFailedAfterTest.class, AfterMethodFailedTest.class })
	@SuppressWarnings("unchecked")
	public void agent_should_not_report_skipped_test_in_case_of_failed_after_method(Class<?> test) {
		TestUtils.runTests(Collections.singletonList(SkippedTestExtension.class), test);

		verify(launch, times(1)).start(); // Start launch
		verify(launch, times(1)).startTestItem(any());  // Start parent suites
		verify(launch, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startItemCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		// 1 before method, 1 test, 1 after method
		verify(launch, times(3)).startTestItem(same(testClassUuid), startItemCapture.capture());

		List<StartTestItemRQ> startItems = startItemCapture.getAllValues();
		assertThat(startItems.get(0).getType(), equalTo(TestMethodType.BEFORE_METHOD.name()));
		assertThat(startItems.get(1).getType(), equalTo(TestMethodType.STEP.name()));
		assertThat(startItems.get(2).getType(), equalTo(TestMethodType.AFTER_METHOD.name()));

		ArgumentCaptor<Maybe<String>> finishUuidCapture = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		// 1 before method, 1 test, 1 after method, finish test class, finish suite = 5
		verify(launch, times(5)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

		List<String> finishUuids = finishUuidCapture.getAllValues().stream().map(Maybe::blockingGet).collect(Collectors.toList());
		assertThat(finishUuids, equalTo(finishUuidOrder));

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
		assertThat(finishItems.get(2).getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishItems.get(2).getIssue(), nullValue());

		finishItems.subList(0, 2).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.PASSED.name()));
			assertThat(e.getIssue(), nullValue());
		});

		finishItems.subList(3, finishItems.size()).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.FAILED.name()));
			assertThat(e.getIssue(), nullValue());
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void agent_should_report_skipped_not_issue_before_tests_in_case_of_failed_after_method_and_retries() {
		TestUtils.runTests(Collections.singletonList(SkippedTestExtension.class), RetryFailedRetryAfterTest.class);

		verify(launch, times(1)).start(); // Start launch
		verify(launch, times(1)).startTestItem(any());  // Start parent suites
		verify(launch, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<Maybe<String>> finishUuidCapture = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		// 1 before 1 test 1 after x 3 retries = 9, + 1 callback update in the first before to set 'retry' flag, + test end, + suite end = 12
		verify(launch, times(12)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

		List<Maybe<String>> itemUuids = finishUuidCapture.getAllValues();
		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();

		List<FinishTestItemRQ> befores = IntStream.range(0, itemUuids.size())
				.mapToObj(i -> Pair.of(itemUuids.get(i), finishItems.get(i)))
				.filter(e -> e.getKey().blockingGet().startsWith("before"))
				.map(Pair::getValue)
				.collect(Collectors.toList());

		// first passed before and update with a retry flag
		befores.subList(0, 2).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.PASSED.name()));
			assertThat(e.getIssue(), nullValue());
		});

		// all other befores should be skipped with "not issue" flag
		befores.subList(2, befores.size()).forEach(e -> {
			assertThat(e.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
			assertThat(e.getIssue(), allOf(notNullValue(), sameInstance(Launch.NOT_ISSUE)));
		});

		List<FinishTestItemRQ> tests = IntStream.range(0, itemUuids.size())
				.mapToObj(i -> Pair.of(itemUuids.get(i), finishItems.get(i)))
				.filter(e -> e.getKey().blockingGet().startsWith("test"))
				.map(Pair::getValue)
				.collect(Collectors.toList());

		assertThat(tests, hasSize(3));

		assertThat(tests.get(0).getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(tests.get(0).isRetry(), equalTo(Boolean.TRUE));
		assertThat(tests.get(0).getIssue(), sameInstance(Launch.NOT_ISSUE));

		assertThat(tests.get(1).getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(tests.get(1).isRetry(), nullValue());
		assertThat(tests.get(1).getIssue(), nullValue());

		assertThat(tests.get(2).getStatus(), equalTo(ItemStatus.PASSED.name()));
		assertThat(tests.get(2).isRetry(), nullValue());
		assertThat(tests.get(2).getIssue(), nullValue());
	}
}
