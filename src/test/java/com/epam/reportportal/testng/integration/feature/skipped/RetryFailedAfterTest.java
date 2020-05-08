package com.epam.reportportal.testng.integration.feature.skipped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryFailedAfterTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetryFailedAfterTest.class);
	private static final int MAXIMUM_RETRIES = 2;

	@BeforeMethod(description = "Retry test initialization method")
	public void setUp() {
		LOGGER.info("Inside @BeforeMethod setUp step");
	}

	@Test(retryAnalyzer = Retry.class)
	public void retryTest() {
		LOGGER.info("Inside @Test retryTest step");
	}

	@AfterMethod(description = "Retry test tear down")
	public void shutDown() {
		throw new IllegalStateException("Inside @AfterMethod shutDown step");
	}

	public static class Retry implements IRetryAnalyzer {
		private final AtomicInteger retryNumber = new AtomicInteger();

		@Override
		public boolean retry(ITestResult result) {
			int retry = retryNumber.incrementAndGet();
			LOGGER.info("Retry attempt: " + retry);
			return retry <= MAXIMUM_RETRIES;
		}
	}
}
