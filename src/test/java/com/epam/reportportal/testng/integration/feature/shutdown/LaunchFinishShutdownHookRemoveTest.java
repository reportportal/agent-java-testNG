package com.epam.reportportal.testng.integration.feature.shutdown;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.testng.TestNGService;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.MultithreadingUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class LaunchFinishShutdownHookRemoveTest {

	private static class MyTestNgService extends TestNGService {
		public MyTestNgService(ReportPortal reportPortal) {
			super(reportPortal);
		}
	}

	public static void main(String... args) throws InterruptedException {
		int port = Integer.parseInt(args[0]);
		System.out.println("Executing using port: " + port);

		ExecutorService myExecutor = MultithreadingUtils.buildExecutorService("rp-test", 2);

		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setReportingTimeout(5);
		parameters.setBaseUrl("http://localhost:" + port);
		ReportPortal client = ReportPortal.builder().withParameters(parameters).withExecutorService(myExecutor).build();
		MyTestNgService service = new MyTestNgService(client);

		service.startLaunch();
		System.out.println("Launch started, sleeping...");
		Thread.sleep(TimeUnit.SECONDS.toMillis(3));
		System.out.println("Finishing launch.");
		service.finishLaunch();
		System.out.println("Exiting...");
	}
}
