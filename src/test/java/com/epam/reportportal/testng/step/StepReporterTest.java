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
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.ManualStepReportPortalListener;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterSimpleTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.testng.TestNG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StepReporterTest {

	private final String suitedUuid = UUID.randomUUID().toString();
	private final String testClassUuid = UUID.randomUUID().toString();
	private final String testMethodUuid = UUID.randomUUID().toString();

	private final List<String> nestedStepsUuids = new ArrayList<>();
	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		nestedStepsUuids.add(uuid);
		Maybe<ItemCreatedRS> maybe = TestUtils.createMaybe(new ItemCreatedRS(uuid, uuid));
		return maybe;
	};

	@Mock
	private ReportPortalClient reportPortalClient;

	@BeforeEach
	public void initMocks() {
		when(reportPortalClient.startLaunch(any())).thenReturn(TestUtils.createMaybe(new StartLaunchRS("launchUuid", 1L)));

		Maybe<ItemCreatedRS> suiteMaybe = TestUtils.createMaybe(new ItemCreatedRS(suitedUuid, suitedUuid));
		when(reportPortalClient.startTestItem(any())).thenReturn(suiteMaybe);

		Maybe<ItemCreatedRS> testClassMaybe = TestUtils.createMaybe(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(reportPortalClient.startTestItem(eq(suitedUuid), any())).thenReturn(testClassMaybe);

		Maybe<ItemCreatedRS> testMethodMaybe = TestUtils.createMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(reportPortalClient.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodMaybe);

		Maybe<OperationCompletionRS> testMethodFinishMaybe = TestUtils.createMaybe(new OperationCompletionRS());
		when(reportPortalClient.finishTestItem(eq(testMethodUuid), any())).thenReturn(testMethodFinishMaybe);

		Maybe<OperationCompletionRS> testClassFinishMaybe = TestUtils.createMaybe(new OperationCompletionRS());
		when(reportPortalClient.finishTestItem(eq(testClassUuid), any())).thenReturn(testClassFinishMaybe);

		Maybe<OperationCompletionRS> suiteFinishMaybe = TestUtils.createMaybe(new OperationCompletionRS());
		when(reportPortalClient.finishTestItem(eq(suitedUuid), any())).thenReturn(suiteFinishMaybe);

		when(reportPortalClient.finishLaunch(eq("launchUuid"), any())).thenReturn(TestUtils.createMaybe(new OperationCompletionRS()));

		when(reportPortalClient.log(any(MultiPartRequest.class))).thenReturn(TestUtils.createMaybe(new BatchSaveOperatingRS()));

		when(reportPortalClient.startTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());
		when(reportPortalClient.finishTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> TestUtils.createMaybe(new OperationCompletionRS()));

		final ReportPortal reportPortal = ReportPortal.create(reportPortalClient, new ListenerParameters(PropertiesLoader.load()));
		ManualStepReportPortalListener.initReportPortal(reportPortal);
	}

	@AfterEach
	public void cleanup(){
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

		verify(reportPortalClient, timeout(1000).times(1)).finishTestItem(eq(nestedStepsUuids.get(0)), any());
	}

}