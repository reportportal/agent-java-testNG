package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.testng.integration.feature.attributes.ClassLevelAttributesTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import static com.epam.reportportal.testng.integration.util.TestUtils.extractRequest;
import static com.epam.reportportal.testng.integration.util.TestUtils.standardParameters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AttributeTest {

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

	private final ListenerParameters parameters = standardParameters();

	@Mock
	private Launch launch;

	@BeforeEach
	public void initMocks() {
		when(launch.getParameters()).thenReturn(parameters);
		when(launch.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		TestReportPortalListener.initLaunch(launch);
	}

	@Test
	public void verify_class_level_attributes_bypass() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), ClassLevelAttributesTest.class);

		verify(launch, times(1)).startTestItem(any());  // Start parent suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");

		assertThat(testRequest.getAttributes(), hasSize(1));
		ItemAttributesRQ attribute = testRequest.getAttributes().iterator().next();
		assertThat(attribute.getKey(), equalTo("myKey"));
		assertThat(attribute.getValue(), equalTo("myValue"));
	}
}
