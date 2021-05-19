package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepFeatureFailedTest;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepFeaturePassedTest;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepMultiLevelTest;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepWithBeforeEachTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.NestedStepTest.TestListener.*;
import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NestedStepTest {

	public static final String PARAM = "test param";
	public static final String NESTED_STEP_NAME_TEMPLATE = "I am nested step with parameter - '{param}'";
	public static final String METHOD_WITH_INNER_METHOD_NAME_TEMPLATE = "I am method with inner method";
	public static final String INNER_METHOD_NAME_TEMPLATE = "I am - {method}";

	public static class TestListener extends BaseTestNGListener {
		public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

		static final String TEST_SUITE_ID = namedId("suite");
		static final String TEST_CLASS_ID = namedId("class");
		static final String TEST_METHOD_ID = namedId("test");
		static final List<String> STEP_ID_LIST = Stream.generate(() -> namedId("step")).limit(2).collect(Collectors.toList());
		static final List<Pair<String, String>> TEST_STEP_ID_ORDER = Arrays.asList(Pair.of(TEST_METHOD_ID, STEP_ID_LIST.get(0)),
				Pair.of(STEP_ID_LIST.get(0), STEP_ID_LIST.get(1))
		);

		public TestListener() {
			super(new TestNGService(new MemoizingSupplier<>(() -> getLaunch(REPORT_PORTAL_THREAD_LOCAL.get().getParameters()))));
		}

		public static void initReportPortal(ReportPortal reportPortal) {
			REPORT_PORTAL_THREAD_LOCAL.set(reportPortal);
		}

		private static Launch getLaunch(ListenerParameters parameters) {
			ReportPortal reportPortal = REPORT_PORTAL_THREAD_LOCAL.get();
			return reportPortal.newLaunch(TestUtils.launchRQ(parameters));
		}
	}

	@Mock
	public ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, "launchUuid", TEST_SUITE_ID, TEST_CLASS_ID, TEST_METHOD_ID);
		ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
		TestListener.initReportPortal(reportPortal);
	}

	@Test
	public void nestedTest() {
		mockNestedSteps(client, TEST_STEP_ID_ORDER.get(0));

		TestUtils.runTests(singletonList(TestListener.class), NestedStepFeaturePassedTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("PASSED", finishNestedRQ.getStatus());

	}

	@Test
	public void nestedInBeforeMethodTest() {
		mockNestedSteps(client, TEST_STEP_ID_ORDER.get(0));

		TestUtils.runTests(singletonList(TestListener.class), NestedStepWithBeforeEachTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("PASSED", finishNestedRQ.getStatus());

	}

	@Test
	public void failedNestedTest() {
		mockNestedSteps(client, TEST_STEP_ID_ORDER.get(0));

		try {
			TestUtils.runTests(singletonList(TestListener.class), NestedStepFeatureFailedTest.class);
		} catch (Exception ex) {
			//to prevent this test failing
		}

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("FAILED", finishNestedRQ.getStatus());

	}

	@Test
	public void testWithMultiLevelNested() throws NoSuchMethodException {
		mockNestedSteps(client, TEST_STEP_ID_ORDER);

		TestUtils.runTests(singletonList(TestListener.class), NestedStepMultiLevelTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());
		verify(client, timeout(1000).times(1)).startTestItem(same(STEP_ID_LIST.get(0)), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(1)), finishNestedCaptor.capture());

		List<StartTestItemRQ> nestedSteps = nestedStepCaptor.getAllValues();

		nestedSteps.forEach(step -> {
			assertNotNull(step);
			assertFalse(step.isHasStats());
		});

		StartTestItemRQ stepWithInnerStep = nestedSteps.get(0);
		assertEquals(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE, stepWithInnerStep.getName());

		StartTestItemRQ innerStep = nestedSteps.get(1);
		assertEquals("I am - " + NestedStepMultiLevelTest.class.getDeclaredMethod("innerMethod").getName(), innerStep.getName());
	}

}
