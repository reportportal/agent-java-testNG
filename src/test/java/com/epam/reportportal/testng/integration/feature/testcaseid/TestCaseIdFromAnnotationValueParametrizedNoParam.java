package com.epam.reportportal.testng.integration.feature.testcaseid;

import com.epam.reportportal.annotations.TestCaseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;

public class TestCaseIdFromAnnotationValueParametrizedNoParam {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseIdFromAnnotationValueParametrizedNoParam.class);

	@TestCaseId(parametrized = true)
	@Test(dataProvider = "test-data-provider")
	public void testCaseIdFromAnnotationValueParametrized(String strParameter, int intParameter) {
		LOGGER.info("Parameters: String - {}, Int - {}", strParameter, intParameter);
	}

	@DataProvider(name = "test-data-provider")
	public Iterator<Object[]> params() {
		return Arrays.asList(new Object[] { "one", 1 }, new Object[] { "two", 2 }, new Object[] { "three", 3 }).iterator();
	}
}
