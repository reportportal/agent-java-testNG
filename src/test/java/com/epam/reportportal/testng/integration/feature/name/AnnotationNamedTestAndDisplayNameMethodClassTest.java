package com.epam.reportportal.testng.integration.feature.name;

import com.epam.reportportal.annotations.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

@Test(testName = "Name from test annotation")
public class AnnotationNamedTestAndDisplayNameMethodClassTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationNamedTestAndDisplayNameMethodClassTest.class);

	@Test
	@DisplayName(AnnotationNamedDisplayNameMethodClassTest.TEST_NAME_DISPLAY)
	public void testNameDisplayNameMethodTest() {
		LOGGER.info("Inside 'testNameDisplayNameMethodTest' method");
	}

}
