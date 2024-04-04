package com.epam.reportportal.testng;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.TestNgListener;
import com.epam.reportportal.testng.integration.feature.description.DescriptionAnnotatedAndTestNgDescriptionTest;
import com.epam.reportportal.testng.integration.feature.description.DescriptionAnnotatedTest;
import com.epam.reportportal.testng.integration.feature.description.DescriptionTest;
import com.epam.reportportal.testng.integration.feature.name.AnnotationNamedClassTest;
import com.epam.reportportal.testng.integration.feature.name.AnnotationNamedParameterizedClassTest;
import com.epam.reportportal.testng.integration.feature.name.AnnotationNamedTestAndDisplayNameMethodClassTest;
import com.epam.reportportal.testng.integration.feature.name.AnnotationNamedDisplayNameMethodClassTest;
import com.epam.reportportal.testng.integration.feature.name.AnnotationNamedDisplayNameParameterizedClassTest;;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

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

	private final String suitedUuid = namedUuid("suite");
	private final String testClassUuid = namedUuid("class");
	private final String stepUuid = namedUuid("test_");

	@Mock
	private ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, stepUuid);
		ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
		TestNgListener.initReportPortal(reportPortal);
	}

	@Test
	public void test_name_should_be_passed_to_rp_if_specified() {
		runTests(Collections.singletonList(TestNgListener.class), AnnotationNamedClassTest.class);

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
	public void test_name_should_be_passed_to_rp_if_display_name_specified() {
		runTests(Collections.singletonList(TestNgListener.class), AnnotationNamedDisplayNameMethodClassTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testClassUuid), startTestCapture.capture());
		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.getName(), equalTo(AnnotationNamedDisplayNameMethodClassTest.TEST_NAME_DISPLAY));
	}

	@Test
	public void test_name_should_be_passed_to_rp_if_both_test_name_and_display_name_are_specified() {
		runTests(Collections.singletonList(TestNgListener.class), AnnotationNamedTestAndDisplayNameMethodClassTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testClassUuid), startTestCapture.capture());
		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.getName(), equalTo(AnnotationNamedDisplayNameMethodClassTest.TEST_NAME_DISPLAY));
	}

	@Test
	public void test_description_should_be_passed_to_rp_if_specified() {
		runTests(Collections.singletonList(TestNgListener.class), DescriptionTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testClassUuid), startTestCapture.capture());
		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.getDescription(), equalTo(DescriptionTest.TEST_DESCRIPTION));
	}

	@Test
	public void test_annotation_description_should_be_passed_to_rp_if_description_annotation_is_specified() {
		runTests(Collections.singletonList(TestNgListener.class), DescriptionAnnotatedTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testClassUuid), startTestCapture.capture());
		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.getDescription(), equalTo(DescriptionAnnotatedTest.TEST_DESCRIPTION_ANNOTATION));
	}

	@Test
	public void test_annotation_description_should_be_passed_to_rp_if_both_test_description_and_description_annotation_are_specified() {
		runTests(Collections.singletonList(TestNgListener.class), DescriptionAnnotatedAndTestNgDescriptionTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(testClassUuid), startTestCapture.capture());
		StartTestItemRQ startItem = startTestCapture.getAllValues().get(0);

		assertThat(startItem.getDescription(), equalTo(DescriptionAnnotatedTest.TEST_DESCRIPTION_ANNOTATION));
	}

	@Test
	public void test_name_should_be_passed_to_rp_if_specified_parameterized_test() {
		runTests(Collections.singletonList(TestNgListener.class), AnnotationNamedParameterizedClassTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(suitedUuid), startTestCapture.capture()); // Start test class
		verify(client, times(2)).startTestItem(same(testClassUuid), any());

		assertThat(startTestCapture.getValue().getName(), equalTo(AnnotationNamedParameterizedClassTest.TEST_NAME));
	}

	@Test
	public void test_name_should_be_passed_to_rp_if_specified_parameterized_test_with_display_name_annotation() {
		runTests(Collections.singletonList(TestNgListener.class), AnnotationNamedDisplayNameParameterizedClassTest.class);

		verify(client, times(1)).startLaunch(any()); // Start launch
		verify(client, times(1)).startTestItem(any());  // Start parent suites
		verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

		ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(testClassUuid), startTestCapture.capture());

		startTestCapture.getAllValues().forEach(startItem -> {
			assertThat(startItem.getName(), equalTo(AnnotationNamedDisplayNameMethodClassTest.TEST_NAME_DISPLAY));
		});
	}
}
