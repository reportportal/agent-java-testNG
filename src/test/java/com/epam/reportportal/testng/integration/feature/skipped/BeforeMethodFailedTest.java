package com.epam.reportportal.testng.integration.feature.skipped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BeforeMethodFailedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeMethodFailedTest.class);

	@BeforeMethod
	public void beforeMethodFailed() {
		throw new IllegalStateException("Inside @BeforeMethod beforeMethodFailed step");
	}

	@Test
	public void testBeforeMethodFailed() {
		LOGGER.info("Test: testBeforeMethodFailed");
	}

	@AfterMethod
	public void shutDown() {
		LOGGER.info("Inside @AfterMethod shutDown step");
	}
}
