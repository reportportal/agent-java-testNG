package com.epam.reportportal.testng.integration.feature.testcaseid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestCaseIdFromCodeReference {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseIdFromCodeReference.class);
	public static final String STEP_NAME = "testCaseIdFromCodeReference";

	@Test
	public void testCaseIdFromCodeReference() {
		LOGGER.info("Code reference: {}.{}", this.getClass().getCanonicalName(), STEP_NAME);
	}
}
