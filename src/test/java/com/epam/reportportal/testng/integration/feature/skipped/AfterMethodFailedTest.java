package com.epam.reportportal.testng.integration.feature.skipped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AfterMethodFailedTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeMethodFailedTest.class);

	@BeforeMethod
	public void beforeMethod() {
		LOGGER.info("Inside @BeforeMethod beforeMethod step");
	}

	@Test
	public void testAfterMethodFailed() {
		LOGGER.info("Test: testAfterMethodFailed");
	}

	@AfterMethod
	public void failedAfterMethod() {
		throw new IllegalStateException("Inside @AfterMethod failedAfterMethod step");
	}
}
