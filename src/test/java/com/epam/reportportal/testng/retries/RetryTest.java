package com.epam.reportportal.testng.retries;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.testng.integration.RetryListener;
import com.epam.reportportal.testng.integration.feature.retry.BasicRetryTest;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RetryTest {

	private final Maybe<String> suitedUuid = createMaybe(namedUuid("suite"));
	private final Maybe<String> testClassUuid = createMaybe(namedUuid("class"));
	List<Maybe<String>> testUuidList = Stream.concat(Stream.concat(
			Stream.generate(() -> createMaybe(namedUuid("before1"))).limit(3),
			Stream.generate(() -> createMaybe(namedUuid("before2"))).limit(3)
	), Stream.concat(
			Stream.generate(() -> createMaybe(namedUuid("test"))).limit(3),
			Stream.generate(() -> createMaybe(namedUuid("after"))).limit(3)
	)).collect(Collectors.toList());

	private final List<String> finishMethodUuidOrder = Stream.of(
			// BASIC CASE
			// 2 before methods
			testUuidList.get(0), testUuidList.get(3),
			// 2 before methods updates, after a test failure, to mark them as retries
			testUuidList.get(0), testUuidList.get(3),
			// 1 test
			testUuidList.get(6),
			// 1 after
			testUuidList.get(9),
			// RETRY 1, no before updates anymore, since we know this is a retry
			testUuidList.get(1), testUuidList.get(4), testUuidList.get(7), testUuidList.get(10),
			// RETRY 2
			testUuidList.get(2), testUuidList.get(5), testUuidList.get(8), testUuidList.get(11),
			// Finish execution
			testClassUuid, suitedUuid
	).map(Maybe::blockingGet).collect(Collectors.toList());

	@Mock
	private Launch launch;
	@Mock
	private StepReporter reporter;

	@BeforeEach
	public void initMocks() {
		List<Maybe<String>> startMethodUuidOrder = Arrays.asList(
				// BASIC CASE
				// 2 before methods
				testUuidList.get(0), testUuidList.get(3),
				// 1 test
				testUuidList.get(6),
				// 1 after
				testUuidList.get(9),
				// RETRY 1
				testUuidList.get(1), testUuidList.get(4), testUuidList.get(7), testUuidList.get(10),
				// RETRY 2
				testUuidList.get(2), testUuidList.get(5), testUuidList.get(8), testUuidList.get(11)
		);
		mockLaunch(launch, reporter, createMaybe("launchUuid"), suitedUuid, testClassUuid, startMethodUuidOrder);
		RetryListener.initLaunch(launch);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_retry_test_with_two_before_methods() {
		runTests(Collections.singletonList(RetryListener.class), BasicRetryTest.class);

		verify(launch, times(1)).start(); // Start launch
		verify(launch, times(1)).startTestItem(any());  // Start parent suites
		verify(launch, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startItemCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		// 2 before methods, 1 test, 1 after method, 3 retries for all of them = 12 start methods
		verify(launch, times(12)).startTestItem(same(testClassUuid), startItemCapture.capture());
		List<StartTestItemRQ> startItems = startItemCapture.getAllValues();

		startItems.subList(0, 3).forEach(e -> assertThat(e.isRetry(), nullValue()));
		startItems.subList(3, startItems.size()).forEach(e -> assertThat(e.isRetry(), equalTo(Boolean.TRUE)));

		ArgumentCaptor<Maybe<String>> finishUuidCapture = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		// 2 before methods, 1 test, 1 after method, 3 retries for all of them = 12 finish methods + 2 call back updates for before method
		// + finish test class, finish suite = 16 finish methods
		verify(launch, times(16)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());
		List<String> finishUuids = finishUuidCapture.getAllValues().stream().map(Maybe::blockingGet).collect(Collectors.toList());
		assertThat(finishUuids, equalTo(finishMethodUuidOrder));

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();

		// we save actual objects, so no this field will be set, but actually it will be sent as null
		finishItems.subList(0, 2).forEach(e -> assertThat(e.isRetry(), equalTo(Boolean.TRUE)));

		finishItems.subList(2, 5).forEach(e -> assertThat(e.isRetry(), equalTo(Boolean.TRUE)));
		finishItems.subList(5, finishItems.size()).forEach(e -> assertThat(e.isRetry(), nullValue()));
	}
}
