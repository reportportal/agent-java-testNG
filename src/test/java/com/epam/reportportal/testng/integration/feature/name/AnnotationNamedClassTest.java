package com.epam.reportportal.testng.integration.feature.name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

@Test(testName = "My test name")
public class AnnotationNamedClassTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationNamedClassTest.class);

	public static final String TEST_NAME = "My test name";

	@Test
	public void testNameTest() {
		LOGGER.info("Inside 'testNameTest' method");
	}

}
