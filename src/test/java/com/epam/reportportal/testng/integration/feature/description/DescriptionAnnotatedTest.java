package com.epam.reportportal.testng.integration.feature.description;

import com.epam.reportportal.annotations.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class DescriptionAnnotatedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionAnnotatedTest.class);

	public static final String TEST_DESCRIPTION_ANNOTATION = "My annotated test description";

	@Test
	@Description(TEST_DESCRIPTION_ANNOTATION)
	public void testDescriptionAnnotatedTest() {
		LOGGER.info("Inside 'testDescriptionAnnotatedTest' method");
	}

}
