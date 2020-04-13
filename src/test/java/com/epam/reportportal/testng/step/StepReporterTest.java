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
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Supplier;
import io.reactivex.Maybe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.*;

import static com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest.FIRST_NAME;
import static com.epam.reportportal.testng.integration.feature.step.ManualStepReporterFeatureTest.SECOND_NAME;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporterTest {

	private final String suitedUuid = UUID.randomUUID().toString();
	private final String testClassUuid = UUID.randomUUID().toString();
	private final String testMethodUuid = UUID.randomUUID().toString();

	@Before
	public void initMocks() {
		ReportPortalClient reportPortalClient = mock(ReportPortalClient.class);

		when(reportPortalClient.startLaunch(any())).thenReturn(TestUtils.createMaybe(new StartLaunchRS("launchUuid", 1L)));

		Maybe<ItemCreatedRS> suiteMaybe = TestUtils.createMaybe(new ItemCreatedRS(suitedUuid, suitedUuid));
		when(reportPortalClient.startTestItem(any())).thenReturn(suiteMaybe);

		Maybe<ItemCreatedRS> testClassMaybe = TestUtils.createMaybe(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(reportPortalClient.startTestItem(eq(suiteMaybe.blockingGet().getId()), any())).thenReturn(testClassMaybe);

		Maybe<ItemCreatedRS> testMethodMaybe = TestUtils.createMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(reportPortalClient.startTestItem(eq(testClassMaybe.blockingGet().getId()), any())).thenReturn(testMethodMaybe);

		when(reportPortalClient.log(any(MultiPartRequest.class))).thenReturn(TestUtils.createMaybe(new BatchSaveOperatingRS()));
		when(reportPortalClient.log(any(SaveLogRQ.class))).thenReturn(TestUtils.createMaybe(new EntryCreatedAsyncRS("logId")));

		final ReportPortal reportPortal = ReportPortal.create(reportPortalClient, new ListenerParameters(PropertiesLoader.load()));
		ManualStepReportPortalListener.initReportPortal(reportPortal);
	}

	@Test
	public void manualStepReporterTest() {

		ReportPortalClient client = ManualStepReportPortalListener.getReportPortal().getClient();

		List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();

		Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
			String uuid = UUID.randomUUID().toString();
			Maybe<ItemCreatedRS> maybe = TestUtils.createMaybe(new ItemCreatedRS(uuid, uuid));
			createdStepsList.add(maybe);
			return maybe;
		};

		when(client.startTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());

		TestUtils.runTests(Collections.singletonList(ManualStepReportPortalListener.class), ManualStepReporterFeatureTest.class);

		verify(client, times(1)).startTestItem(any());  // Start parent suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(eq(testMethodUuid), captor.capture()); // Start test class and test method

		ArgumentCaptor<MultiPartRequest> multiPartRequestArgumentCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(client, times(3)).log(multiPartRequestArgumentCaptor.capture());

		Map<String, List<SaveLogRQ>> logsMapping = multiPartRequestArgumentCaptor.getAllValues()
				.stream()
				.flatMap(request -> request.getSerializedRQs().stream())
				.map(serialized -> (List<SaveLogRQ>) serialized.getRequest())
				.flatMap(Collection::stream)
				.collect(groupingBy(SaveLogRQ::getItemUuid));

		String firstStepUuid = createdStepsList.get(0).blockingGet().getId();
		String secondStepUuid = createdStepsList.get(1).blockingGet().getId();

		List<SaveLogRQ> testMethodLogs = logsMapping.get(testMethodUuid);
		List<SaveLogRQ> firstStepLogs = logsMapping.get(firstStepUuid);
		List<SaveLogRQ> secondStepLogs = logsMapping.get(secondStepUuid);

		assertEquals(1, testMethodLogs.size());
		assertEquals(2, firstStepLogs.size());
		assertEquals(1, secondStepLogs.size());

		assertEquals("ERROR", testMethodLogs.get(0).getLevel());
		assertEquals("INFO", firstStepLogs.get(0).getLevel());
		assertEquals("INFO", firstStepLogs.get(1).getLevel());
		assertEquals("ERROR", secondStepLogs.get(0).getLevel());

		assertEquals("[main] - Main test method error log\n", testMethodLogs.get(0).getMessage());
		assertEquals("[main] - First info log of the first step\n", firstStepLogs.get(0).getMessage());
		assertEquals("[main] - Second info log of the first step\n", firstStepLogs.get(1).getMessage());
		assertEquals("[main] - First error log of the second step\n", secondStepLogs.get(0).getMessage());

		List<StartTestItemRQ> nestedSteps = captor.getAllValues();

		nestedSteps.stream().map(StartTestItemRQ::isHasStats).forEach(Assert::assertFalse);

		StartTestItemRQ firstStepRequest = nestedSteps.get(0);
		StartTestItemRQ secondStepRequest = nestedSteps.get(1);

		assertEquals(FIRST_NAME, firstStepRequest.getName());
		assertEquals(SECOND_NAME, secondStepRequest.getName());

	}

}