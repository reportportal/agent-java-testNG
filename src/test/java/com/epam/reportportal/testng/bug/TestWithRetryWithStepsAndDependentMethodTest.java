package com.epam.reportportal.testng.bug;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGService;
import com.epam.reportportal.testng.integration.bug.RetryWithStepsAndDependentMethodTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithRetryWithStepsAndDependentMethodTest {

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
			rq.setStartTime(Calendar.getInstance().getTime());
			rq.setMode(parameters.getLaunchRunningMode());
			rq.setStartTime(Calendar.getInstance().getTime());

			return reportPortal.newLaunch(rq);

		}
	}

	private final String suitedUuid = namedUuid("suite");
	private final String testClassUuid = namedUuid("class");
	private final List<String> testUuidList = Arrays.asList(namedUuid("test1"), namedUuid("test1"), namedUuid("test2"));
	private final List<String> nestedStepUuidList = Arrays.asList(namedUuid("tst1-ns1-"), namedUuid("tst1-ns2-"), namedUuid("tst2-ns1-"));

	private final List<Pair<String, String>> testStepUuidOrder = IntStream.range(0, 3)
			.mapToObj(i -> Pair.of(testUuidList.get(i), nestedStepUuidList.get(i)))
			.collect(Collectors.toList());

	private final List<String> finishUuidOrder = Arrays.asList(
			nestedStepUuidList.get(0),
			testUuidList.get(0),
			nestedStepUuidList.get(1),
			testUuidList.get(1),
			nestedStepUuidList.get(2),
			testUuidList.get(2),
			testClassUuid,
			suitedUuid
	);

	@Mock
	private ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, testUuidList);
		TestUtils.mockNestedSteps(client, testStepUuidOrder);
		ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
		TestListener.initReportPortal(reportPortal);
	}

	private static void verifyPositiveFinish(List<String> finishUuids, List<FinishTestItemRQ> finishItems) {
		IntStream.range(0, finishItems.size()).forEach(i -> {
			String uuid = finishUuids.get(i);
			FinishTestItemRQ item = finishItems.get(i);
			assertThat("FinishTestItemRQ for uuid '" + uuid + "' incorrect retry flag.", item.isRetry(), nullValue());
			assertThat("FinishTestItemRQ for uuid '" + uuid + "' incorrect status.", item.getStatus(), equalTo(ItemStatus.PASSED.name()));
		});
	}

	@Test
	public void verify_second_test_passes_in_case_of_retry() {
		runTests(Collections.singletonList(TestListener.class), RetryWithStepsAndDependentMethodTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(3)).startTestItem(same(testClassUuid), startTestCapture.capture());
		List<StartTestItemRQ> startItems = startTestCapture.getAllValues();

		assertThat(startItems.get(0).isRetry(), nullValue());
		assertThat(startItems.get(1).isRetry(), equalTo(Boolean.TRUE));
		assertThat(startItems.get(2).isRetry(), nullValue());

		ArgumentCaptor<StartTestItemRQ> startStepCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testUuidList.get(0)), startStepCapture.capture());
		verify(client, times(1)).startTestItem(same(testUuidList.get(1)), startStepCapture.capture());
		verify(client, times(1)).startTestItem(same(testUuidList.get(2)), startStepCapture.capture());

		ArgumentCaptor<String> finishUuidCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(8)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());
		List<String> finishUuids = finishUuidCapture.getAllValues();
		assertThat(finishUuids, equalTo(finishUuidOrder));

		List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
		assertThat(finishItems.get(1).isRetry(), equalTo(Boolean.TRUE));
		assertThat(finishItems.get(1).getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(finishItems.get(1).getIssue(), sameInstance(Launch.NOT_ISSUE));

		verifyPositiveFinish(finishUuidOrder.subList(0, 1), finishItems.subList(0, 1));
		verifyPositiveFinish(finishUuidOrder.subList(2, finishUuidOrder.size()), finishItems.subList(2, finishItems.size()));
	}
}
