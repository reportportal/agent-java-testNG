package com.epam.reportportal.testng;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.TestNgListener;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepFeatureFailedTest;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepFeaturePassedTest;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepMultiLevelTest;
import com.epam.reportportal.testng.integration.feature.nested.NestedStepWithBeforeEachTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
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

	private final String testSuiteId = namedId("suite_");
	private final String testClassId = namedId("class_");
	private final String testMethodId = namedId("test_");
	private final List<String> stepIdList = Stream.generate(() -> namedId("step_")).limit(2).collect(Collectors.toList());
	private final List<Pair<String, String>> testStepIdOrder = Arrays.asList(Pair.of(testMethodId, stepIdList.get(0)),
			Pair.of(stepIdList.get(0), stepIdList.get(1))
	);

	@Mock
	public ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, "launchUuid", testSuiteId, testClassId, testMethodId);
		TestNgListener.initReportPortal(ReportPortal.create(client, standardParameters()));
	}

	@Test
	public void nestedTest() {
		mockNestedSteps(client, testStepIdOrder.get(0));

		TestUtils.runTests(singletonList(TestNgListener.class), NestedStepFeaturePassedTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(testMethodId), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(stepIdList.get(0)), finishNestedCaptor.capture());

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
		mockNestedSteps(client, testStepIdOrder.get(0));

		TestUtils.runTests(singletonList(TestNgListener.class), NestedStepWithBeforeEachTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(testMethodId), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(stepIdList.get(0)), finishNestedCaptor.capture());

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
		mockNestedSteps(client, testStepIdOrder.get(0));

		try {
			TestUtils.runTests(singletonList(TestNgListener.class), NestedStepFeatureFailedTest.class);
		} catch (Exception ex) {
			//to prevent this test failing
		}

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(testMethodId), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(stepIdList.get(0)), finishNestedCaptor.capture());

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
		mockNestedSteps(client, testStepIdOrder);

		TestUtils.runTests(singletonList(TestNgListener.class), NestedStepMultiLevelTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(same(testMethodId), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(stepIdList.get(0)), finishNestedCaptor.capture());
		verify(client, timeout(1000).times(1)).startTestItem(same(stepIdList.get(0)), nestedStepCaptor.capture());
		verify(client, timeout(1000).times(1)).finishTestItem(same(stepIdList.get(1)), finishNestedCaptor.capture());

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
