package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.feature.description.DescriptionTest;
import com.epam.reportportal.testng.integration.feature.name.AnnotationNamedClassTest;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rp.com.google.common.base.Suppliers;

import java.util.Calendar;
import java.util.Collections;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestNameAndDescriptionTest {
	public static class TestListener extends BaseTestNGListener {
		public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

		public TestListener() {
			super(new TestNGService(Suppliers.memoize(() -> getLaunch(REPORT_PORTAL_THREAD_LOCAL.get().getParameters()))));
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
	private final String testUuid = namedUuid("test");

	@Mock
	private ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, testUuid);
		ReportPortal reportPortal = ReportPortal.create(client, new ListenerParameters(PropertiesLoader.load()));
		TestListener.initReportPortal(reportPortal);
	}

	@Test
	public void test_name_should_be_passed_to_rp_if_specified() {
		runTests(Collections.singletonList(TestListener.class), AnnotationNamedClassTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(suitedUuid), startTestCapture.capture()); // Start test class
		verify(client, times(1)).startTestItem(same(testClassUuid), any()); // Start test step

		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.isRetry(), nullValue());
		assertThat(startItem.getName(), equalTo(AnnotationNamedClassTest.TEST_NAME));
	}

	@Test
	public void test_description_should_be_passed_to_rp_if_specified() {
		runTests(Collections.singletonList(TestListener.class), DescriptionTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testClassUuid), startTestCapture.capture());
		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.getDescription(), equalTo(DescriptionTest.TEST_DESCRIPTION));
	}
}
