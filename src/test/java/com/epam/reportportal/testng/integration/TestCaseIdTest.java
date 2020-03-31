package com.epam.reportportal.testng.integration;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromAnnotationValue;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromAnnotationValueParametrized;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromCodeRefAndParams;
import com.epam.reportportal.testng.integration.feature.testcaseid.TestCaseIdFromCodeReference;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.extractRequest;
import static com.epam.reportportal.testng.integration.util.TestUtils.extractRequests;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestCaseIdTest {

	@Before
	public void initMocks() {
		TestReportPortalListener.launch = mock(Launch.class);
		when(TestReportPortalListener.launch.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(TestReportPortalListener.launch.startTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
	}

	@Test
	public void testCaseIdFromCodeRef() {
		TestUtils.runTests(TestCaseIdFromCodeReference.class);

		String expectedCodeRef = TestCaseIdFromCodeReference.class.getCanonicalName() + "." + TestCaseIdFromCodeReference.STEP_NAME;

		Launch launch = TestReportPortalListener.launch;

		verify(launch, times(1)).startTestItem(any());  // Start parent suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		StartTestItemRQ stepRequest = extractRequest(captor, "step");

		assertEquals(TestUtils.TEST_NAME, testRequest.getName());
		assertEquals(expectedCodeRef, stepRequest.getCodeRef());
		assertEquals(expectedCodeRef, stepRequest.getTestCaseId());
	}

	@Test
	public void testCaseIdFromCodeRefAndParams() {
		TestUtils.runTests(TestCaseIdFromCodeRefAndParams.class);

		String expectedCodeRef = TestCaseIdFromCodeRefAndParams.class.getCanonicalName() + "." + TestCaseIdFromCodeReference.STEP_NAME;
		List<String> expectedTestCaseIds = Stream.of("one", "two", "three")
				.map(it -> expectedCodeRef + "[" + it + "]")
				.collect(Collectors.toList());

		Launch launch = TestReportPortalListener.launch;

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(any(), captor.capture()); // Start test and steps

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		List<String> actualTestCaseIds = extractRequests(captor, "step").stream()
				.map(StartTestItemRQ::getTestCaseId)
				.collect(Collectors.toList());

		assertEquals(TestUtils.TEST_NAME, testRequest.getName());
		assertEquals(expectedTestCaseIds, actualTestCaseIds);
	}

	@Test
	public void testCaseIdFromAnnotationValue() {
		TestUtils.runTests(TestCaseIdFromAnnotationValue.class);

		Launch launch = TestReportPortalListener.launch;

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		StartTestItemRQ stepRequest = extractRequest(captor, "step");

		assertEquals(TestUtils.TEST_NAME, testRequest.getName());
		assertEquals(TestCaseIdFromAnnotationValue.TEST_CASE_ID, stepRequest.getTestCaseId());
	}

	@Test
	public void testCaseIdFromAnnotationValueParametrized() {
		TestUtils.runTests(TestCaseIdFromAnnotationValueParametrized.class);

		Launch launch = TestReportPortalListener.launch;

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		List<StartTestItemRQ> stepRequests = extractRequests(captor, "step");
		List<String> actualTestCaseIds = stepRequests.stream().map(StartTestItemRQ::getTestCaseId).collect(Collectors.toList());

		assertEquals(TestUtils.TEST_NAME, testRequest.getName());
		assertThat(actualTestCaseIds, containsInAnyOrder("one", "two", "three"));
	}
}
