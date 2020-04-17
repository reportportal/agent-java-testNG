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
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import rp.com.google.common.io.ByteStreams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class FileLocatorTest {

	private final String testLaunchUuid = "launch" + UUID.randomUUID().toString().substring(6);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);
	private final Maybe<String> launchUuid = TestUtils.createMaybe(testLaunchUuid);

	@Mock
	public ReportPortalClient client;

	private final StepReporter sr = StepReporter.getInstance();

	private final List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();
	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		Maybe<ItemCreatedRS> maybe = TestUtils.createMaybe(new ItemCreatedRS(uuid, uuid));
		createdStepsList.add(maybe);
		return maybe;
	};

	@BeforeEach
	public void initMocks() {
		Maybe<ItemCreatedRS> testMethodCreatedMaybe = TestUtils.createMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);

		ListenerParameters params = new ListenerParameters(PropertiesLoader.load());
		ReportPortal rp = ReportPortal.create(client, params);
		Launch launch = rp.withLaunch(launchUuid);
		sr.setLaunch(launch);
		Maybe<String> methodMaybe = launch.startTestItem(TestUtils.createMaybe(testClassUuid), TestUtils.standardStartStepRequest());
		sr.setParent(methodMaybe);
	}

	@Test
	public void test_file_location_by_relative_workdir_path() throws IOException {
		// create a nested step
		maybeSupplier.get();

		// mock start nested steps
		when(client.startTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(0));

		sr.sendStep("Test image by relative workdir path", new File("src/test/resources/pug/lucky.jpg"));

		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(client, after(1000).times(1)).log(logCaptor.capture());

		MultiPartRequest logRq = logCaptor.getValue();
		verifyFile(logRq, ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("pug/lucky.jpg")), "lucky.jpg");
	}

	@Test
	public void test_file_location_by_relative_classpath_path() throws IOException {
		// create a nested step
		maybeSupplier.get();

		// mock start nested steps
		when(client.startTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(0));

		sr.sendStep("Test image by relative classpath path", new File("pug/unlucky.jpg"));

		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(client, after(1000).times(1)).log(logCaptor.capture());

		MultiPartRequest logRq = logCaptor.getValue();
		verifyFile(logRq, ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("pug/unlucky.jpg")), "unlucky.jpg");
	}

	@Test
	public void test_file_not_found_in_path() {

		// create a nested step
		maybeSupplier.get();

		// mock start nested steps
		when(client.startTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(0));

		sr.sendStep("Test image by relative classpath path", new File("pug/not_exists.jpg"));

		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(client, after(1000).times(1)).log(logCaptor.capture());

		MultiPartRequest logRq = logCaptor.getValue();
		SaveLogRQ saveRq = verifyRq(logRq);
		assertThat(saveRq.getFile(), nullValue());
	}

	private void verifyFile(MultiPartRequest logRq, byte[] data, String fileName) {
		SaveLogRQ saveRq = verifyRq(logRq);

		assertThat(saveRq.getFile(), notNullValue());
		assertThat("File name is invalid", saveRq.getMessage(), equalTo(fileName));
		SaveLogRQ.File file = saveRq.getFile();
		assertThat("File binary content is invalid", file.getContent(), equalTo(data));
	}

	@SuppressWarnings("unchecked")
	private SaveLogRQ verifyRq(MultiPartRequest logRq) {
		List<MultiPartRequest.MultiPartSerialized<?>> mpRqs = logRq.getSerializedRQs();
		assertThat(mpRqs, hasSize(1));

		Object rqs = mpRqs.get(0).getRequest();
		assertThat(rqs, instanceOf(List.class));
		List<Object> rqList = (List<Object>) rqs;
		assertThat(rqList, hasSize(1));

		Object rq = rqList.get(0);
		assertThat(rq, instanceOf(SaveLogRQ.class));
		return (SaveLogRQ) rq;
	}
}
