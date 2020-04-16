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

import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.ManualStepReportPortalListener;
import com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class StepReporterFailureTest {
	private final String suitedUuid = "suite" + UUID.randomUUID().toString().substring(5);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);

	@Mock
	private ReportPortalClient reportPortalClient;

	private final List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();

	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		Maybe<ItemCreatedRS> maybe = TestUtils.createMaybe(new ItemCreatedRS(uuid, uuid));
		createdStepsList.add(maybe);
		return maybe;
	};

	private final AtomicInteger counter = new AtomicInteger();

	private static final ErrorRS ERROR_RS;

	static {
		ERROR_RS = new ErrorRS();
		ERROR_RS.setErrorType(ErrorType.INCORRECT_REQUEST);
		ERROR_RS.setMessage("Incorrect Request. [Value is not allowed for field 'status'.]");
	}

	private static final ReportPortalException EXCEPTION = new ReportPortalException(400, "Bad Request", ERROR_RS);

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
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
		when(reportPortalClient.log(any(SaveLogRQ.class))).thenReturn(TestUtils.createMaybe(new EntryCreatedAsyncRS("logId")));

		final ReportPortal reportPortal = ReportPortal.create(reportPortalClient, new ListenerParameters(PropertiesLoader.load()));
		ManualStepReportPortalListener.initReportPortal(reportPortal);
	}

	@Test
	public void failed_to_finish_nested_step_should_not_hang_the_launch() {
		ReportPortalClient client = ManualStepReportPortalListener.getReportPortal().getClient();

		// create 3 nested steps
		maybeSupplier.get();
		maybeSupplier.get();
		maybeSupplier.get();

		// mock start nested steps
		when(client.startTestItem(
				eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(counter.getAndIncrement()));

		// Finish first nested steps and throw an exception on second
		when(client.finishTestItem(eq(testMethodUuid), any())).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(
				0)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(1)).thenThrow(EXCEPTION);

		TestUtils.runTests(Collections.singletonList(ManualStepReportPortalListener.class), ManualStepReporterFeatureTest.class);

		// First nested step finish
		verify(client, times(1)).finishTestItem(eq(createdStepsList.get(0).blockingGet().getUniqueId()), any());
		// Second nested step finish
		verify(client, times(1)).finishTestItem(eq(createdStepsList.get(1).blockingGet().getUniqueId()), any());
		verify(client, times(1)).finishTestItem(eq(testMethodUuid), any()); // Test finish
		verify(client, times(1)).finishTestItem(eq(testClassUuid), any()); // Test class finish
		verify(client, times(1)).finishTestItem(eq(suitedUuid), any()); // Suite finish
		verify(client, times(1)).finishLaunch(eq("launchUuid"), any()); // Launch finish
	}
}
