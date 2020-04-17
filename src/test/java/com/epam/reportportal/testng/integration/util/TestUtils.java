package com.epam.reportportal.testng.integration.util;

import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.mockito.ArgumentCaptor;
import org.testng.ITestNGListener;
import org.testng.TestNG;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestUtils {

	public static final String TEST_NAME = "TestContainer";

	public static void runTests(List<Class<? extends ITestNGListener>> listeners, Class... classes) {
		final TestNG testNG = new TestNG(true);
		testNG.setListenerClasses(listeners);
		testNG.setTestClasses(classes);
		testNG.setDefaultTestName(TEST_NAME);
		testNG.setExcludedGroups("optional");
		testNG.run();
	}

	public static Maybe<String> createMaybeUuid() {
		return createMaybe(UUID.randomUUID().toString());
	}

	public static <T> Maybe<T> createMaybe(T id) {
		return Maybe.create(emitter -> {
			emitter.onSuccess(id);
			emitter.onComplete();
		});
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
		rq.setStartTime(Calendar.getInstance().getTime());
		String id = generateUniqueId();
		rq.setName("Step_" + id);
		rq.setDescription("Test step description");
		rq.setUniqueId(id);
		rq.setType("STEP");
		return rq;
	}
}
