package com.epam.reportportal.testng.integration.feature.skipped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

public class BeforeClassFailedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeClassFailedTest.class);

	@BeforeClass
	public void beforeClassFailed() {
		throw new IllegalStateException("Inside @BeforeClass beforeClassFailed step");
	}

	@Test
	public void testBeforeMethodFailed() {
		LOGGER.info("Test: testBeforeClassFailed");
	}

	@AfterClass
	public void shutDown() {
		LOGGER.info("Inside @AfterClass shutDown step");
	}
}
