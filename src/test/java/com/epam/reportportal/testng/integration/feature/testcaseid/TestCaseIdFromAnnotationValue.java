package com.epam.reportportal.testng.integration.feature.testcaseid;

import com.epam.reportportal.annotations.TestCaseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestCaseIdFromAnnotationValue {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseIdFromAnnotationValue.class);
	public static final String TEST_CASE_ID = "test-case-id";

	@TestCaseId(TEST_CASE_ID)
	@Test
	public void testCaseIdFromAnnotationValueTest() {
		LOGGER.info("Test case id value from annotation: {}", TEST_CASE_ID);
	}
}
