package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.analytics.AnalyticsService;
import com.epam.reportportal.testng.integration.GoogleAnalyticsListener;
import com.epam.reportportal.testng.integration.feature.analytics.AnalyticsTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsIntegrationTest {

	@Mock
	private ReportPortalClient reportPortalClient;

	@Mock
	private AnalyticsService analyticsService;

	@BeforeEach
	public void initMocks() {
		when(reportPortalClient.startLaunch(any())).thenReturn(TestUtils.createMaybe(new StartLaunchRS("launchUuid", 1L)));

		ReportPortal reportPortal = ReportPortal.create(reportPortalClient, new ListenerParameters(PropertiesLoader.load()));
		GoogleAnalyticsListener.initReportPortal(reportPortal);
		GoogleAnalyticsListener.initAnalyticsService(analyticsService);
	}

	@Test
	public void googleAnalyticsEventTest() {
		try {
			TestUtils.runTests(Collections.singletonList(GoogleAnalyticsListener.class), AnalyticsTest.class);
		} catch (Exception ex) {
			//do nothing
			ex.printStackTrace();
		}

		ArgumentCaptor<StartLaunchRQ> startLaunchCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(analyticsService, times(1)).sendEvent(any(Maybe.class), startLaunchCaptor.capture());

		StartLaunchRQ startRq = startLaunchCaptor.getValue();

		List<String> attributes = startRq.getAttributes()
				.stream()
				.filter(ItemAttributesRQ::isSystem)
				.map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.toList());
		assertThat(attributes, hasItem("agent:agent-java-testng|test-version-1"));
	}
}
