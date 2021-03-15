package com.epam.reportportal.testng;

import com.epam.reportportal.testng.integration.feature.shutdown.LaunchFinishShutdownHookRemoveTest;
import com.epam.reportportal.testng.integration.feature.shutdown.LaunchFinishShutdownHookTest;
import com.epam.reportportal.util.test.ProcessUtils;
import com.epam.reportportal.util.test.SocketUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestLaunchFinishShutdownHook {

	// TODO: fix LaunchFinishShutdownHookTest.class parameter
	@ParameterizedTest
	@ValueSource(classes = { LaunchFinishShutdownHookRemoveTest.class })
	public void test_shutdown_hook_finishes_launch_on_java_machine_exit(final Class<?> clazz) throws Exception {

		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(ss, Collections.emptyMap(), "files/socket_response.txt");
		Callable<Process> clientCallable = () -> ProcessUtils.buildProcess(true, clazz, String.valueOf(ss.getLocalPort()));
		Pair<String, Process> startResult = SocketUtils.executeServerCallable(serverCallable, clientCallable);
		assertThat(startResult.getValue(), notNullValue());
		assertThat("First request is a launch start", startResult.getKey(), startsWith("POST /api/v1/test-project/launch"));

		Callable<Integer> clientCallableResult = () -> {
			try {
				if (startResult.getValue().waitFor(7, TimeUnit.SECONDS)) {
					return startResult.getValue().exitValue();
				} else {
					startResult.getValue().destroy();
					return -1;
				}
			} catch (InterruptedException e) {
				return -2;
			}
		};
		Pair<String, Integer> finishResult = SocketUtils.executeServerCallable(serverCallable, clientCallableResult);

		assertThat("Second request is a launch finish",
				finishResult.getKey(),
				startsWith("PUT /api/v1/test-project/launch/b7a79414-287c-452d-b157-c32fe6cb1c72/finish")
		);
		assertThat("Exit code should be '0'", finishResult.getValue(), equalTo(0));
	}
}
