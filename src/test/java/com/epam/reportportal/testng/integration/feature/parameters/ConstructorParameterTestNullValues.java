package com.epam.reportportal.testng.integration.feature.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class ConstructorParameterTestNullValues {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConstructorParameterTestNullValues.class);

	private final String numWord;

	@DataProvider(name = "provider")
	public static Object[][] getData() {
		return new Object[][] { new Object[] { "one" }, new Object[] { "two" }, new Object[] { null } };
	}

	@Factory(dataProvider = "provider")
	public ConstructorParameterTestNullValues(String numberWord) {
		numWord = numberWord;
	}

	@Test
	public void testNumberConversion() {
		LOGGER.info("Test " + numWord);
		assertThat(numWord, notNullValue());
	}
}
