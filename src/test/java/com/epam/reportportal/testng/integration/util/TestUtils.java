package com.epam.reportportal.testng.integration.util;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.testng.ITestNGListener;
import org.testng.TestNG;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestUtils {

	public static final String TEST_NAME = "TestContainer";

	// 10 milliseconds is enough to separate one test from another
	public static final long MINIMAL_TEST_PAUSE = 20L;

	public static TestNG runTests(List<Class<? extends ITestNGListener>> listeners, Class<?>... classes) {
		final TestNG testNG = new TestNG(true);
		testNG.setListenerClasses(listeners);
		testNG.setTestClasses(classes);
		testNG.setDefaultTestName(TEST_NAME);
		testNG.setExcludedGroups("optional");
		testNG.run();
		return testNG;
	}

	public static StartTestItemRQ extractRequest(ArgumentCaptor<StartTestItemRQ> captor, String methodType) {
		return captor.getAllValues()
				.stream()
				.filter(it -> methodType.equalsIgnoreCase(it.getType()))
				.findAny()
				.orElseThrow(() -> new AssertionError(String.format("Method type '%s' should be present among requests", methodType)));
	}

	public static List<StartTestItemRQ> extractRequests(ArgumentCaptor<StartTestItemRQ> captor, String methodType) {
		return captor.getAllValues().stream().filter(it -> methodType.equalsIgnoreCase(it.getType())).collect(Collectors.toList());
	}

	/**
	 * Generates a unique ID shorter than UUID based on current time in milliseconds and thread ID.
	 *
	 * @return a unique ID string
	 */
	public static String generateUniqueId() {
		return System.currentTimeMillis() + "-" + Thread.currentThread().getId() + "-" + ThreadLocalRandom.current().nextInt(9999);
	}

	public static StartTestItemRQ standardStartStepRequest() {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setStartTime(Instant.now());
		String id = generateUniqueId();
		rq.setName("Step_" + id);
		rq.setDescription("Test step description");
		rq.setUniqueId(id);
		rq.setType("STEP");
		return rq;
	}

	public static void mockLaunch(Launch launch, Maybe<String> launchUuid, Maybe<String> suiteUuid, Maybe<String> testClassUuid,
			Collection<Maybe<String>> testMethodUuidList) {
		mockLaunch(launch, standardParameters(), launchUuid, suiteUuid, testClassUuid, testMethodUuidList);
	}

	@SuppressWarnings("unchecked")
	public static void mockLaunch(Launch launch, ListenerParameters parameters, Maybe<String> launchUuid, Maybe<String> suiteUuid,
			Maybe<String> testClassUuid, Collection<Maybe<String>> testMethodUuidList) {
		when(launch.getParameters()).thenReturn(parameters);

		when(launch.start()).thenReturn(launchUuid);
		when(launch.startTestItem(any())).thenReturn(suiteUuid);
		when(launch.startTestItem(same(suiteUuid), any())).thenReturn(testClassUuid);

		Iterator<Maybe<String>> methodIterator = testMethodUuidList.iterator();
		Maybe<String> first = methodIterator.next();
		List<Maybe<String>> methodMaybes = new ArrayList<>();
		methodIterator.forEachRemaining(methodMaybes::add);
		Maybe<String>[] other = methodMaybes.toArray(new Maybe[0]);
		when(launch.startTestItem(same(testClassUuid), any())).thenReturn(first, other);

		new HashSet<>(testMethodUuidList).forEach(methodUuidMaybe -> when(launch.finishTestItem(same(methodUuidMaybe), any())).thenReturn(
				Maybe.just(new OperationCompletionRS())));
		when(launch.finishTestItem(same(testClassUuid), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(launch.finishTestItem(same(suiteUuid), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
	}

	public static void mockLaunch(ReportPortalClient client, String launchUuid, String suiteUuid, String testClassUuid,
			String testMethodUuid) {
		mockLaunch(client, launchUuid, suiteUuid, testClassUuid, Collections.singleton(testMethodUuid));
	}

	public static String namedUuid(String name) {
		return name + UUID.randomUUID().toString().substring(name.length());
	}

	@SuppressWarnings("unchecked")
	public static void mockLaunch(ReportPortalClient client, String launchUuid, String suiteUuid, String testClassUuid,
			Collection<String> testMethodUuidList) {
		when(client.startLaunch(any())).thenReturn(Maybe.just(new StartLaunchRS(launchUuid, 1L)));

		Maybe<ItemCreatedRS> suiteMaybe = Maybe.just(new ItemCreatedRS(suiteUuid, suiteUuid));
		when(client.startTestItem(any())).thenReturn(suiteMaybe);

		Maybe<ItemCreatedRS> testClassMaybe = Maybe.just(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(client.startTestItem(eq(suiteUuid), any())).thenReturn(testClassMaybe);

		List<Maybe<ItemCreatedRS>> responses = testMethodUuidList.stream()
				.map(uuid -> Maybe.just(new ItemCreatedRS(uuid, uuid)))
				.collect(Collectors.toList());
		Maybe<ItemCreatedRS> first = responses.get(0);
		Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(first, other);
		new HashSet<>(testMethodUuidList).forEach(testMethodUuid -> when(client.finishTestItem(
				eq(testMethodUuid),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS())));

		Maybe<OperationCompletionRS> testClassFinishMaybe = Maybe.just(new OperationCompletionRS());
		when(client.finishTestItem(eq(testClassUuid), any())).thenReturn(testClassFinishMaybe);

		Maybe<OperationCompletionRS> suiteFinishMaybe = Maybe.just(new OperationCompletionRS());
		when(client.finishTestItem(eq(suiteUuid), any())).thenReturn(suiteFinishMaybe);

		when(client.finishLaunch(eq(launchUuid), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
	}

	public static void mockNestedSteps(ReportPortalClient client, Pair<String, String> parentNestedPair) {
		mockNestedSteps(client, Collections.singletonList(parentNestedPair));
	}

	@SuppressWarnings("unchecked")
	public static void mockNestedSteps(final ReportPortalClient client, final List<Pair<String, String>> parentNestedPairs) {
		Map<String, List<String>> responseOrders = parentNestedPairs.stream()
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		responseOrders.forEach((k, v) -> {
			List<Maybe<ItemCreatedRS>> responses = v.stream()
					.map(uuid -> Maybe.just(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());

			Maybe<ItemCreatedRS> first = responses.get(0);
			Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
			when(client.startTestItem(same(k), any())).thenReturn(first, other);
		});
		parentNestedPairs.forEach(p -> when(client.finishTestItem(same(p.getValue()),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> Maybe.just(new OperationCompletionRS())));
	}

	@SuppressWarnings("unchecked")
	public static void mockLogging(ReportPortalClient client) {
		when(client.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setBaseUrl("http://localhost:8080");
		result.setLaunchName("My-test-launch" + CommonUtils.generateUniqueId());
		result.setProjectName("test-project");
		result.setEnable(true);
		return result;
	}

	public static StartLaunchRQ launchRQ(ListenerParameters parameters) {
        StartLaunchRQ result = new StartLaunchRQ();
        result.setName(parameters.getLaunchName());
        result.setStartTime(Instant.now());
		result.setMode(parameters.getLaunchRunningMode());
		return result;
	}
}
