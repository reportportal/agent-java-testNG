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
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest.FIRST_NAME;
import static com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest.SECOND_NAME;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporterItemStartTest {

	private final String suitedUuid = UUID.randomUUID().toString();
	private final String testClassUuid = UUID.randomUUID().toString();
	private final String testMethodUuid = UUID.randomUUID().toString();

	@Mock
	private ReportPortalClient reportPortalClient;;

	@BeforeEach
	public void initMocks() {
		when(reportPortalClient.startLaunch(any())).thenReturn(TestUtils.createMaybe(new StartLaunchRS("launchUuid", 1L)));

		Maybe<ItemCreatedRS> suiteMaybe = TestUtils.createMaybe(new ItemCreatedRS(suitedUuid, suitedUuid));
		when(reportPortalClient.startTestItem(any())).thenReturn(suiteMaybe);

		Maybe<ItemCreatedRS> testClassMaybe = TestUtils.createMaybe(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(reportPortalClient.startTestItem(eq(suiteMaybe.blockingGet().getId()), any())).thenReturn(testClassMaybe);

		Maybe<ItemCreatedRS> testMethodMaybe = TestUtils.createMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(reportPortalClient.startTestItem(eq(testClassMaybe.blockingGet().getId()), any())).thenReturn(testMethodMaybe);

		when(reportPortalClient.log(any(MultiPartRequest.class))).thenReturn(TestUtils.createMaybe(new BatchSaveOperatingRS()));

		final ReportPortal reportPortal = ReportPortal.create(reportPortalClient, new ListenerParameters(PropertiesLoader.load()));
		ManualStepReportPortalListener.initReportPortal(reportPortal);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void manualStepReporterTest() {
		List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();

		Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
			String uuid = UUID.randomUUID().toString();
			Maybe<ItemCreatedRS> maybe = TestUtils.createMaybe(new ItemCreatedRS(uuid, uuid));
			createdStepsList.add(maybe);
			return maybe;
		};

		when(reportPortalClient.startTestItem(eq(testMethodUuid), any())).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());

		TestUtils.runTests(Collections.singletonList(ManualStepReportPortalListener.class), ManualStepReporterFeatureTest.class);

		verify(reportPortalClient, times(1)).startTestItem(any());  // Start parent suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(reportPortalClient, times(3)).startTestItem(eq(testMethodUuid), captor.capture()); // Start test class and test method

		ArgumentCaptor<MultiPartRequest> multiPartRequestArgumentCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(reportPortalClient, timeout(1000).times(6)).log(multiPartRequestArgumentCaptor.capture());

		Map<String, List<SaveLogRQ>> logsMapping = multiPartRequestArgumentCaptor.getAllValues()
				.stream()
				.flatMap(request -> request.getSerializedRQs().stream())
				.map(serialized -> (List<SaveLogRQ>) serialized.getRequest())
				.flatMap(Collection::stream)
				.collect(groupingBy(SaveLogRQ::getItemUuid));

		String firstStepUuid = createdStepsList.get(0).blockingGet().getId();
		String secondStepUuid = createdStepsList.get(1).blockingGet().getId();
		String thirdStepUuid = createdStepsList.get(2).blockingGet().getId();

		List<SaveLogRQ> testMethodLogs = logsMapping.get(testMethodUuid);
		List<SaveLogRQ> firstStepLogs = logsMapping.get(firstStepUuid);
		List<SaveLogRQ> secondStepLogs = logsMapping.get(secondStepUuid);
		List<SaveLogRQ> thirdStepLogs = logsMapping.get(thirdStepUuid);

		assertThat(testMethodLogs, hasSize(1));
		assertThat(firstStepLogs, hasSize(2));
		assertThat(secondStepLogs, hasSize(1));
		assertThat(thirdStepLogs, hasSize(2));

		assertEquals("ERROR", testMethodLogs.get(0).getLevel());
		assertEquals("INFO", firstStepLogs.get(0).getLevel());
		assertEquals("INFO", firstStepLogs.get(1).getLevel());
		assertEquals("ERROR", secondStepLogs.get(0).getLevel());
		assertThat(thirdStepLogs.stream().map(SaveLogRQ::getLevel).collect(Collectors.toList()), hasItems("ERROR", "INFO"));

		assertTrue(testMethodLogs.get(0).getMessage().contains("Main test method error log"));
		assertTrue(firstStepLogs.get(0).getMessage().contains("First info log of the first step"));
		assertTrue(firstStepLogs.get(1).getMessage().contains("Second info log of the first step"));
		assertTrue(secondStepLogs.get(0).getMessage().contains("First error log of the second step"));
		assertTrue(thirdStepLogs.get(0).getMessage().contains("unlucky.jpg"));
		assertTrue(thirdStepLogs.get(1).getMessage().contains("Second error log of the second step"));

		List<StartTestItemRQ> nestedSteps = captor.getAllValues();

		nestedSteps.stream().map(StartTestItemRQ::isHasStats).forEach(Assertions::assertFalse);

		StartTestItemRQ firstStepRequest = nestedSteps.get(0);
		StartTestItemRQ secondStepRequest = nestedSteps.get(1);

		assertEquals(FIRST_NAME, firstStepRequest.getName());
		assertEquals(SECOND_NAME, secondStepRequest.getName());

	}

}