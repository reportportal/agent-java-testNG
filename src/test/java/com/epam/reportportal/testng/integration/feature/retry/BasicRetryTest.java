package com.epam.reportportal.testng.integration.feature.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class BasicRetryTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicRetryTest.class);
	private static final int MAXIMUM_RETRIES = 2;

	private final AtomicInteger testRetryNumber = new AtomicInteger();

	@BeforeMethod(description = "Retry test initialization method")
	public void setUp1() {
		LOGGER.info("Inside @BeforeMethod setUp1 step");
	}

	@BeforeMethod(description = "Retry test second initialization method")
	public void setUp2() {
		LOGGER.info("Inside @BeforeMethod setUp2 step");
	}

	@Test(retryAnalyzer = Retry.class)
	public void retryTest() {
		int retry = testRetryNumber.incrementAndGet();
		if (retry <= MAXIMUM_RETRIES) {
			LOGGER.warn("Failed attempt: " + retry);
			Assert.fail();
		}
		LOGGER.info("Success attempt");
	}

	@AfterMethod(description = "Retry test tear down")
	public void shutDown() {
		LOGGER.info("Inside @AfterMethod shutDown step");
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
