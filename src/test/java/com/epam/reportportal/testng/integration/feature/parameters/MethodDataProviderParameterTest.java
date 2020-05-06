package com.epam.reportportal.testng.integration.feature.parameters;

import com.ibm.icu.text.RuleBasedNumberFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MethodDataProviderParameterTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodDataProviderParameterTest.class);

	@DataProvider(name = "provider")
	public static Object[][] getData() {
		return new Object[][] { new Object[] { 1, "one" }, new Object[] { 2, "two" } };
	}

	@Test(dataProvider = "provider")
	public void testNumberConversion(int num, String numWord) {
		LOGGER.info("Number: " + num + "; expected result: " + numWord);
		Locale us = Locale.US;
		RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(us, RuleBasedNumberFormat.SPELLOUT);
		String result = formatter.format(num, "%spellout-numbering");
		assertThat(result, equalTo(numWord));
	}
}
