package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.analytics.GoogleAnalytics;
import com.epam.reportportal.service.analytics.item.AnalyticsItem;
import com.epam.reportportal.testng.integration.GoogleAnalyticsListener;
import com.epam.reportportal.testng.integration.feature.analytics.AnalyticsTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsIntegrationTest {

	@BeforeEach
	public void initMocks() {
		ReportPortalClient reportPortalClient = mock(ReportPortalClient.class);

		when(reportPortalClient.startLaunch(any())).thenReturn(TestUtils.createMaybe(new StartLaunchRS("launchUuid", 1L)));

		final ReportPortal reportPortal = ReportPortal.create(reportPortalClient, new ListenerParameters(PropertiesLoader.load()));

		GoogleAnalytics googleAnalytics = mock(GoogleAnalytics.class);
		GoogleAnalyticsListener.initReportPortal(reportPortal);
		GoogleAnalyticsListener.initGoogleAnalytics(googleAnalytics);
	}

	@Test
	public void googleAnalyticsEventTest() {
		GoogleAnalytics googleAnalytics = GoogleAnalyticsListener.getGoogleAnalytics();

		try {
			TestUtils.runTests(Collections.singletonList(GoogleAnalyticsListener.class), AnalyticsTest.class);
		} catch (Exception ex) {
			//do nothing
			ex.printStackTrace();
		}

		ArgumentCaptor<AnalyticsItem> argumentCaptor = ArgumentCaptor.forClass(AnalyticsItem.class);
		verify(googleAnalytics, times(1)).send(argumentCaptor.capture());

		AnalyticsItem value = argumentCaptor.getValue();

		Map<String, String> params = value.getParams();

		String type = params.get("t");
		String eventAction = params.get("ea");
		String eventCategory = params.get("ec");
		String eventLabel = params.get("el");

		assertEquals("event", type);
		assertEquals("Start launch", eventAction);
		assertTrue(eventCategory.contains("client-java"));
		assertEquals("test-agent|test-version-1", eventLabel);

	}
}
