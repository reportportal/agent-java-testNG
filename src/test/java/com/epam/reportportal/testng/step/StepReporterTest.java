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

package com.epam.reportportal.testng.step;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.ManualStepReportPortalListener;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterSimpleTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.testng.TestNG;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StepReporterTest {

	private final String suitedUuid = TestUtils.namedUuid("suite");
	private final String testClassUuid = TestUtils.namedUuid("class");
	private final String testMethodUuid = TestUtils.namedUuid("test");

	@Mock
	private ReportPortalClient client;

	private List<String> nestedStepsUuids;

	@BeforeEach
	public void initMocks() {
		TestUtils.mockLaunch(client, "launchUuid", suitedUuid, testClassUuid, testMethodUuid);
		nestedStepsUuids = TestUtils.mockNestedSteps(client, testMethodUuid);
		final ReportPortal reportPortal = ReportPortal.create(client, new ListenerParameters(PropertiesLoader.load()));
		ManualStepReportPortalListener.initReportPortal(reportPortal);
	}

	@AfterEach
	public void cleanup() {
		nestedStepsUuids.clear();
	}

	@Test
	public void verify_failed_nested_step_fails_test_run() {
		TestNG testNg = TestUtils.runTests(Collections.singletonList(ManualStepReportPortalListener.class),
				ManualStepReporterFeatureTest.class
		);

		assertThat(testNg.hasFailure(), equalTo(Boolean.TRUE));
	}

	@Test
	public void verify_listener_finishes_unfinished_step() {
		TestUtils.runTests(Collections.singletonList(ManualStepReportPortalListener.class), ManualStepReporterSimpleTest.class);

		verify(client, timeout(1000).times(1)).finishTestItem(eq(nestedStepsUuids.get(0)), any());
	}

}