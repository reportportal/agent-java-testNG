package com.epam.reportportal.testng.integration.feature.shutdown;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.testng.TestNGService;
import com.epam.reportportal.testng.integration.util.TestUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LaunchFinishShutdownHookTest {

	private static class MyTestNgService extends TestNGService {
		public MyTestNgService() {
			super();
		}

		protected void setRp(ReportPortal rp) {
			TestNGService.setReportPortal(rp);
		}
	}

	public static void main(String... args) throws InterruptedException {
		int port = Integer.parseInt(args[0]);
		System.out.println("Executing using port: " + port);

		ExecutorService myExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});

		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setBaseUrl("http://localhost:" + port);
		ReportPortal client = ReportPortal.builder().withParameters(parameters).withExecutorService(myExecutor).build();
		MyTestNgService service = new MyTestNgService();
		service.setRp(client);

		service.startLaunch();
		System.out.println("Launch started, sleeping...");
		Thread.sleep(TimeUnit.SECONDS.toMillis(5));
		System.out.println("Exiting...");
	}

}
