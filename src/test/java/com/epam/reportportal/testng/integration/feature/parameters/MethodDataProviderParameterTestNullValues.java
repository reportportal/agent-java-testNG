package com.epam.reportportal.testng.integration.feature.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class MethodDataProviderParameterTestNullValues {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodDataProviderParameterTestNullValues.class);

	@DataProvider(name = "provider")
	public static Object[][] getData() {
		return new Object[][] { new Object[] { "one" }, new Object[] { "two" }, new Object[] { null } };
	}

	@Test(dataProvider = "provider")
	public void testNumberConversion(String numWord) {
		LOGGER.info("Test " + numWord);
		assertThat(numWord, notNullValue());
	}
}
