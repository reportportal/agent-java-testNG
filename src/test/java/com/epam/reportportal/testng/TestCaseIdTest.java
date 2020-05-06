package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.testng.integration.TestReportPortalListener;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromAnnotationValue;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromAnnotationValueParametrized;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromCodeRefAndParams;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromCodeReference;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.extractRequest;
import static com.epam.reportportal.testng.integration.util.TestUtils.extractRequests;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestCaseIdTest {

	@Mock
	private Launch launch;
	@Mock
	private ListenerParameters parameters;
	@Mock
	private StepReporter stepReporter;

	@BeforeEach
	public void initMocks() {
		when(launch.getParameters()).thenReturn(parameters);
		when(launch.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(launch.getStepReporter()).thenReturn(stepReporter);
		TestReportPortalListener.initLaunch(launch);
		when(parameters.isCallbackReportingEnabled()).thenReturn(Boolean.TRUE);
	}

	@Test
	public void testCaseIdFromCodeRef() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), TestCaseIdFromCodeReference.class);

		String expectedCodeRef = TestCaseIdFromCodeReference.class.getCanonicalName() + "." + TestCaseIdFromCodeReference.STEP_NAME;

		verify(launch, times(1)).startTestItem(any());  // Start parent suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		StartTestItemRQ stepRequest = extractRequest(captor, "step");

		assertThat(testRequest.getName(), equalTo(TestUtils.TEST_NAME));
		assertThat(stepRequest.getCodeRef(), equalTo(expectedCodeRef));
		assertThat(stepRequest.getTestCaseId(), equalTo(expectedCodeRef));
	}

	@Test
	public void testCaseIdFromCodeRefAndParams() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), TestCaseIdFromCodeRefAndParams.class);

		String expectedCodeRef = TestCaseIdFromCodeRefAndParams.class.getCanonicalName() + "." + TestCaseIdFromCodeReference.STEP_NAME;
		List<String> expectedTestCaseIds = Stream.of("one", "two", "three")
				.map(it -> expectedCodeRef + "[" + it + "]")
				.collect(Collectors.toList());

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(any(), captor.capture()); // Start test and steps

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		List<String> actualTestCaseIds = extractRequests(captor, "step").stream()
				.map(StartTestItemRQ::getTestCaseId)
				.collect(Collectors.toList());

		assertThat(testRequest.getName(), equalTo(TestUtils.TEST_NAME));
		assertThat(actualTestCaseIds, equalTo(expectedTestCaseIds));
	}

	@Test
	public void testCaseIdFromAnnotationValue() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), TestCaseIdFromAnnotationValue.class);

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		StartTestItemRQ stepRequest = extractRequest(captor, "step");

		assertThat(testRequest.getName(), equalTo(TestUtils.TEST_NAME));
		assertThat(stepRequest.getTestCaseId(), equalTo(TestCaseIdFromAnnotationValue.TEST_CASE_ID));
	}

	@Test
	public void testCaseIdFromAnnotationValueParametrized() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), TestCaseIdFromAnnotationValueParametrized.class);

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		List<StartTestItemRQ> stepRequests = extractRequests(captor, "step");
		List<String> actualTestCaseIds = stepRequests.stream().map(StartTestItemRQ::getTestCaseId).collect(Collectors.toList());

		assertThat(testRequest.getName(), equalTo(TestUtils.TEST_NAME));
		assertThat(actualTestCaseIds, containsInAnyOrder("one", "two", "three"));
	}
}
