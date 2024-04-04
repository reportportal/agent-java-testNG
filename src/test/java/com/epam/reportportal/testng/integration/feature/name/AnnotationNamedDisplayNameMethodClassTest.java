package com.epam.reportportal.testng.integration.feature.name;

import com.epam.reportportal.annotations.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class AnnotationNamedDisplayNameMethodClassTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationNamedDisplayNameMethodClassTest.class);

	public static final String TEST_NAME_DISPLAY = "My display test name";

	@Test
	@DisplayName(TEST_NAME_DISPLAY)
	public void testNameDisplayNameMethodTest() {
		LOGGER.info("Inside 'testNameDisplayNameMethodTest' method");
	}

}
