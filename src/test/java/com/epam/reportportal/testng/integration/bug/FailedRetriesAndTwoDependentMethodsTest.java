package com.epam.reportportal.testng.integration.bug;

import org.testng.Assert;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.epam.reportportal.testng.integration.util.TestUtils.MINIMAL_TEST_PAUSE;

public class FailedRetriesAndTwoDependentMethodsTest {
	private static final int MAXIMUM_RETRIES = 1;
	private final AtomicInteger testRetryNumber = new AtomicInteger();

	@Test(retryAnalyzer = Retry.class)
	public void retryTest() throws InterruptedException {
		Thread.sleep(MINIMAL_TEST_PAUSE);
		int retry = testRetryNumber.incrementAndGet();
		System.out.println("Failed attempt: " + retry);
		Assert.fail();
	}

	@Test(dependsOnMethods = "retryTest")
	public void dependencyOnRetry() throws InterruptedException {
		Thread.sleep(MINIMAL_TEST_PAUSE);
		System.out.println("Dependent test");
	}

	@Test(dependsOnMethods = "dependencyOnRetry")
	public void dependencyOnDependency() throws InterruptedException {
		Thread.sleep(MINIMAL_TEST_PAUSE);
		System.out.println("Dependent on dependent test");
	}

	public static class Retry implements IRetryAnalyzer {
		private final AtomicInteger retryNumber = new AtomicInteger();

		@Override
		public boolean retry(ITestResult result) {
			int retry = retryNumber.incrementAndGet();
			System.out.println("Retry attempt: " + retry);
			return retry <= MAXIMUM_RETRIES;
		}
	}
}
