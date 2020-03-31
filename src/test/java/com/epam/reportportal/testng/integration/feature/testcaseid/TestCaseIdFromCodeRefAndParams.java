package com.epam.reportportal.testng.integration.feature.testcaseid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestCaseIdFromCodeRefAndParams {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseIdFromCodeRefAndParams.class);
	public static final String STEP_NAME = "testCaseIdFromCodeReference";

	@Test(dataProvider = "test-data-provider")
	public void testCaseIdFromCodeReference(String strParameter) {
		LOGGER.info("Code reference: {}.{}", this.getClass().getCanonicalName(), STEP_NAME);
		LOGGER.info("Parameters: String - {}", strParameter);
	}

	@DataProvider(name = "test-data-provider")
	public Iterator<Object> params() {
		return Arrays.asList((Object) "one", (Object) "two", (Object) "three").iterator();
	}

}
