package com.epam.reportportal.testng.integration.feature.testcaseid;

import com.epam.reportportal.annotations.TestCaseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;

public class TestCaseIdFromAnnotationValueNotParametrizedNoParam {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseIdFromAnnotationValueNotParametrizedNoParam.class);
	public static final String TEST_CASE_ID = "test-case-id";

	@TestCaseId(TEST_CASE_ID)
	@Test(dataProvider = "test-data-provider")
	public void testCaseIdFromAnnotationValueParametrized(String strParameter, int intParameter) {
		LOGGER.info("Parameters: String - {}, Int - {}", strParameter, intParameter);
	}

	@DataProvider(name = "test-data-provider")
	public Iterator<Object[]> params() {
		return Arrays.asList(new Object[] { "one", 1 }, new Object[] { "two", 2 }, new Object[] { "three", 3 }).iterator();
	}
}
