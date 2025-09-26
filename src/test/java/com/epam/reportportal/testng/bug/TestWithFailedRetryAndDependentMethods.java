package com.epam.reportportal.testng.bug;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGService;
import com.epam.reportportal.testng.integration.bug.FailedRetriesAndTwoDependentMethodsTest;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithFailedRetryAndDependentMethods {

	public static class TestListener extends BaseTestNGListener {
		public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

		public TestListener() {
			super(new TestNGService(new MemoizingSupplier<>(() -> getLaunch(REPORT_PORTAL_THREAD_LOCAL.get().getParameters()))));
		}

		public static void initReportPortal(ReportPortal reportPortal) {
			REPORT_PORTAL_THREAD_LOCAL.set(reportPortal);
		}

		private static Launch getLaunch(ListenerParameters parameters) {

			ReportPortal reportPortal = REPORT_PORTAL_THREAD_LOCAL.get();
			StartLaunchRQ rq = new StartLaunchRQ();
			rq.setName(parameters.getLaunchName());
			rq.setStartTime(Instant.now());
			rq.setMode(parameters.getLaunchRunningMode());
			rq.setStartTime(Instant.now());

			return reportPortal.newLaunch(rq);

		}
	}

	private final String suitedUuid = namedUuid("suite");
	private final String testClassUuid = namedUuid("class");
	private final List<String> testUuidList = Arrays.asList(namedUuid("test1"), namedUuid("test1"), namedUuid("test2"), namedUuid("test3"));

	private final List<String> finishUuidOrder = Stream.concat(testUuidList.stream(), Stream.of(testClassUuid, suitedUuid))
			.collect(Collectors.toList());

	@Mock
	private ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, testUuidList);
		mockLogging(client);
		ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
		TestListener.initReportPortal(reportPortal);
	}

	@Test
	public void verify_second_test_passes_in_case_of_retry() {
		runTests(Collections.singletonList(TestListener.class), FailedRetriesAndTwoDependentMethodsTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(4)).startTestItem(same(testClassUuid), startTestCapture.capture());
		List<StartTestItemRQ> startItems = startTestCapture.getAllValues();

		assertThat(startItems.get(1).isRetry(), equalTo(Boolean.TRUE));
		Stream.concat(startItems.subList(0, 1).stream(), startItems.subList(2, startItems.size()).stream())
				.forEach(i -> assertThat(i.isRetry(), nullValue()));

		ArgumentCaptor<String> finishUuidCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(finishUuidOrder.size())).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());
		List<String> finishUuids = finishUuidCapture.getAllValues();
		assertThat(finishUuids, equalTo(finishUuidOrder));

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
		assertThat(finishItems.get(0).isRetry(), equalTo(Boolean.TRUE));
		assertThat(finishItems.get(0).getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(finishItems.get(0).getIssue(), notNullValue());
		assertThat(finishItems.get(0).getIssue().getIssueType(), equalTo(Launch.NOT_ISSUE.getIssueType()));

		assertThat(finishItems.get(1).isRetry(), nullValue());
		assertThat(finishItems.get(1).getStatus(), equalTo(ItemStatus.FAILED.name()));

		finishItems.subList(2, 4).forEach(i -> {
			assertThat(i.isRetry(), nullValue());
			assertThat(i.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		});

		finishItems.subList(4, finishItems.size() - 2).forEach(i -> {
			assertThat(i.isRetry(), nullValue());
			assertThat(i.getStatus(), equalTo(ItemStatus.FAILED.name()));
		});

		finishItems.subList(finishItems.size() - 2, finishItems.size()).forEach(i -> {
			assertThat(i.isRetry(), nullValue());
			assertThat(i.getStatus(), nullValue());
		});
	}
}
