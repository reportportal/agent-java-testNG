package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.feature.parameters.MethodDataProviderParameterTest;
import com.epam.reportportal.testng.integration.feature.parameters.MethodDataProviderParameterTestNullValues;
import com.epam.reportportal.testng.integration.feature.parameters.ParameterNamesTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rp.com.google.common.base.Suppliers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.testng.integration.util.TestUtils.namedUuid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParametersBypassTest {
	public static class TestReportPortalListener extends BaseTestNGListener {
		public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

		public TestReportPortalListener() {
			super(new TestNGServiceExtension(
					Suppliers.memoize(() -> getLaunch(REPORT_PORTAL_THREAD_LOCAL.get().getParameters())),
					REPORT_PORTAL_THREAD_LOCAL.get()
			));
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
	private final List<String> testMethodUuidList = Arrays.asList(namedUuid("test"), namedUuid("test"));

	@Mock
	private ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		TestUtils.mockLaunch(client, "launchUuid", suitedUuid, testClassUuid, testMethodUuidList);

		final ReportPortal reportPortal = ReportPortal.create(client, new ListenerParameters(PropertiesLoader.load()));
		TestReportPortalListener.initReportPortal(reportPortal);
	}

	private static void verify_parameters(List<StartTestItemRQ> methodStarts) {
		assertThat(methodStarts, hasSize(2));

		methodStarts.stream().map(StartTestItemRQ::getParameters).peek(pl -> assertThat(pl, hasSize(2))).forEach(testParams -> {
			assertThat(testParams, hasSize(2));
			assertThat(testParams.get(0).getKey(), equalTo(Integer.TYPE.getCanonicalName()));
			assertThat(testParams.get(1).getKey(), equalTo(String.class.getCanonicalName()));

			assertThat(testParams.get(0).getValue() + "-" + testParams.get(1).getValue(), anyOf(equalTo("1-one"), equalTo("2-two")));
		});
	}

	private static void verify_null_parameter(List<StartTestItemRQ> methodStarts) {
		assertThat(methodStarts, hasSize(3));
		String strClassName = String.class.getCanonicalName();
		List<String> expectedKeys = Collections.nCopies(3, strClassName);
		List<String> expectedValues = Arrays.asList("one", "two", "NULL");
		List<String> expectedResult = IntStream.range(0, 3)
				.mapToObj(i -> expectedKeys.get(i) + "-" + expectedValues.get(i))
				.collect(Collectors.toList());

		IntStream.range(0, 3).forEach(i -> {
			List<ParameterResource> params = methodStarts.get(i).getParameters();
			assertThat(params, hasSize(1));
			ParameterResource param = params.get(0);
			assertThat(expectedResult, hasItem(param.getKey() + "-" + param.getValue()));
		});
	}

	@Test
	public void verify_method_data_provider_parameters_bypass() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), MethodDataProviderParameterTest.class);

		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(2)).startTestItem(startsWith("class"), testCaptor.capture());

		verify_parameters(testCaptor.getAllValues());
	}

	@Test
	public void verify_method_data_provider_parameters_bypass_with_null() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), MethodDataProviderParameterTestNullValues.class);

		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(3)).startTestItem(startsWith("class"), testCaptor.capture());

		verify_null_parameter(testCaptor.getAllValues());
	}

	@Test
	public void verify_parameter_key_annotation_bypass_for_test_method() {
		TestUtils.runTests(Collections.singletonList(TestReportPortalListener.class), ParameterNamesTest.class);

		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(2)).startTestItem(startsWith("class"), testCaptor.capture());

		List<StartTestItemRQ> methodStarts = testCaptor.getAllValues();

		assertThat(methodStarts, hasSize(2));

		methodStarts.stream().map(StartTestItemRQ::getParameters).peek(pl -> assertThat(pl, hasSize(2))).forEach(testParams -> {
			assertThat(testParams, hasSize(2));
			assertThat(testParams.get(0).getKey(), equalTo(ParameterNamesTest.FIRST_PARAMETER_NAME));
			assertThat(testParams.get(1).getKey(), equalTo(ParameterNamesTest.SECOND_PARAMETER_NAME));

			assertThat(testParams.get(0).getValue() + "-" + testParams.get(1).getValue(), anyOf(equalTo("1-one"), equalTo("2-two")));
		});
	}
}
