/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.TestNgListener;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterSimpleTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.testng.TestNG;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StepReporterTest {

	private final String suitedUuid = namedUuid("suite");
	private final String testClassUuid = namedUuid("class");
	private final String testMethodUuid = namedUuid("test");
	private final List<String> stepUuidList = Stream.generate(() -> namedUuid("step")).limit(3).collect(Collectors.toList());
	private final List<Pair<String, String>> testStepUuidOrder = stepUuidList.stream()
			.map(u -> Pair.of(testMethodUuid, u))
			.collect(Collectors.toList());

	@Mock
	private ReportPortalClient client;

	@BeforeEach
	public void initMocks() {
		mockLaunch(client, "launchUuid", suitedUuid, testClassUuid, testMethodUuid);
		mockLogging(client);
		ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
		TestNgListener.initReportPortal(reportPortal);
	}

	/*
	 * Failing test from nested steps causes more issues than solve, so strictly prohibited.
	 * 1. If test is going to be retried, marking a test result as a failure makes TestNG skip dependent tests.
	 * 2. If we want to retry a test which failed by a nested step, then we should set a 'wasRetried' flag and test result status as 'SKIP'.
	 *    TestNG actually will not retry such test as it should, and mark all dependent tests as skipped.
	 *
	 * DO NOT DELETE OR MODIFY THIS TEST, SERIOUSLY!
	 */
	@Test
	public void verify_failed_nested_step_not_fails_test_run() {
		mockNestedSteps(client, testStepUuidOrder);
		TestNG testNg = runTests(Collections.singletonList(TestNgListener.class), ManualStepReporterFeatureTest.class);

		assertThat(testNg.hasFailure(), equalTo(Boolean.FALSE));
	}

	@Test
	public void verify_listener_finishes_unfinished_step() {
		mockNestedSteps(client, testStepUuidOrder.get(0));
		runTests(Collections.singletonList(TestNgListener.class), ManualStepReporterSimpleTest.class);

		verify(client, timeout(1000).times(1)).finishTestItem(same(stepUuidList.get(0)), any());
	}

}