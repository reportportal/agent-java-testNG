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
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class StepOrderTest {

	private final String testLaunchUuid = "launch" + UUID.randomUUID().toString().substring(6);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);
	private final Maybe<String> launchUuid = TestUtils.createMaybe(testLaunchUuid);
	private final AtomicInteger counter = new AtomicInteger();

	@Mock
	private ReportPortalClient client;

	private final StepReporter sr = StepReporter.getInstance();

	private final List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();
	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		Maybe<ItemCreatedRS> maybe = TestUtils.createMaybe(new ItemCreatedRS(uuid, uuid));
		createdStepsList.add(maybe);
		return maybe;
	};

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);

		Maybe<ItemCreatedRS> testMethodCreatedMaybe = TestUtils.createMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);
		when(client.startTestItem(eq(testMethodUuid), any())).thenReturn(testMethodCreatedMaybe);

		ListenerParameters params = new ListenerParameters(PropertiesLoader.load());
		params.setBatchLogsSize(1);
		params.setClientJoin(false);
		ReportPortal rp = ReportPortal.create(client, params);
		Launch launch = rp.withLaunch(launchUuid);
		sr.setLaunch(launch);
		Maybe<String> methodMaybe = launch.startTestItem(TestUtils.createMaybe(testClassUuid), TestUtils.standardStartStepRequest());
		sr.setParent(methodMaybe);
	}

	@Test
	public void test_steps_have_different_start_time() {
		int stepNum = 30;

		// create nested steps
		for (int i = 0; i < stepNum; i++) {
			maybeSupplier.get();
		}

		// mock start nested steps
		when(client.startTestItem(
				eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(counter.getAndIncrement()));
		// mock finish nested steps
		when(client.finishTestItem(eq(testMethodUuid), any())).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(
				0));

		for (int i = 0; i < stepNum; i++) {
			sr.sendStep(i + " step");
		}

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(stepNum)).startTestItem(eq(testMethodUuid), stepCaptor.capture());

		List<StartTestItemRQ> rqs = stepCaptor.getAllValues();
		assertThat(rqs, hasSize(stepNum));
		for (int i = 1; i < stepNum; i++) {
			assertThat("Each nested step should not complete in the same millisecond, iteration: " + i,
					rqs.get(i - 1).getStartTime(), not(equalTo(rqs.get(i).getStartTime())));
		}
	}
}
