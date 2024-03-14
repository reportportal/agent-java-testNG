package com.epam.reportportal.testng.integration.feature.description;

import com.epam.reportportal.annotations.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class DescriptionAnnotatedAndTestNgDescriptionTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionAnnotatedAndTestNgDescriptionTest.class);

	@Test(description = DescriptionTest.TEST_DESCRIPTION)
	@Description(DescriptionAnnotatedTest.TEST_DESCRIPTION_ANNOTATION)
	public void testDescriptionAnnotatedAndTestNgDescriptionTest() {
		LOGGER.info("Inside 'testDescriptionAnnotatedAndTestNgDescriptionTest' method");
	}

}
