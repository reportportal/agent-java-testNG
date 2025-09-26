package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.testng.integration.feature.testcaseid.*;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
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
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestCaseIdTest {

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

	@Mock
	private Launch launch;
	@Mock
	private ListenerParameters parameters;

	@BeforeEach
	public void initMocks() {
		when(launch.getParameters()).thenReturn(parameters);
		when(launch.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
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

	@Test
	public void verify_test_case_id_parameterized_no_marked_parameters() {
		TestUtils.runTests(
				Collections.singletonList(TestReportPortalListener.class),
				TestCaseIdFromAnnotationValueParametrizedNoParam.class
		);

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		List<StartTestItemRQ> stepRequests = extractRequests(captor, "step");
		List<String> actualTestCaseIds = stepRequests.stream().map(StartTestItemRQ::getTestCaseId).collect(Collectors.toList());

		assertThat(testRequest.getName(), equalTo(TestUtils.TEST_NAME));
		assertThat(actualTestCaseIds, containsInAnyOrder("[one,1]", "[two,2]", "[three,3]"));
	}

	@Test
	public void verify_test_case_id_not_parameterized_no_marked_parameters() {
		String testCaseId = TestCaseIdFromAnnotationValueNotParametrizedNoParam.TEST_CASE_ID;
		TestUtils.runTests(
				Collections.singletonList(TestReportPortalListener.class),
				TestCaseIdFromAnnotationValueNotParametrizedNoParam.class
		);

		verify(launch, times(1)).startTestItem(any());  // Start parent suites

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(any(), captor.capture()); // Start test and step

		StartTestItemRQ testRequest = extractRequest(captor, "test");
		List<StartTestItemRQ> stepRequests = extractRequests(captor, "step");
		List<String> actualTestCaseIds = stepRequests.stream().map(StartTestItemRQ::getTestCaseId).collect(Collectors.toList());

		assertThat(testRequest.getName(), equalTo(TestUtils.TEST_NAME));
		assertThat(actualTestCaseIds, containsInAnyOrder(testCaseId, testCaseId, testCaseId));
	}

	@Test
	void test_verify_test_case_id_supports_templating_with_self_reference() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), TestCaseIdTemplateTest.class);

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

		List<StartTestItemRQ> stepRequests = captor.getAllValues()
				.stream()
				.filter(rq -> ItemType.STEP.name().equals(rq.getType()))
				.collect(Collectors.toList());
		assertThat(stepRequests, hasSize(1));
		assertThat(
				stepRequests.get(0).getTestCaseId(),
				equalTo(TestCaseIdTemplateTest.TEST_CASE_ID_VALUE.replace("{this.FIELD}", TestCaseIdTemplateTest.FIELD))
		);
	}
}
