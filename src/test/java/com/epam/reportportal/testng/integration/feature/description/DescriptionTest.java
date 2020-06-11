package com.epam.reportportal.testng.integration.feature.description;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class DescriptionTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionTest.class);

	public static final String TEST_DESCRIPTION = "My test description";

	@Test(description = TEST_DESCRIPTION)
	public void testDescriptionTest() {
		LOGGER.info("Inside 'testDescriptionTest' method");
	}

}
