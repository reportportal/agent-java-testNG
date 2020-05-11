package com.epam.reportportal.testng.integration.feature.skipped;

import com.ibm.icu.text.RuleBasedNumberFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BeforeMethodFailedParametrizedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeMethodFailedParametrizedTest.class);

	@DataProvider(name = "provider")
	public static Object[][] getData() {
		return new Object[][] { new Object[] { 1, "one" }, new Object[] { 2, "two" } };
	}

	@BeforeMethod
	public void beforeMethodFailed() {
		throw new IllegalStateException("Inside @BeforeMethod beforeMethodFailed step");
	}

	@Test(dataProvider = "provider")
	public void testBeforeMethodFailed(int num, String numWord) {
		LOGGER.info("Number: " + num + "; expected result: " + numWord);
		Locale us = Locale.US;
		RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(us, RuleBasedNumberFormat.SPELLOUT);
		String result = formatter.format(num, "%spellout-numbering");
		assertThat(result, equalTo(numWord));
	}

	@AfterMethod
	public void shutDown() {
		LOGGER.info("Inside @AfterMethod shutDown step");
	}
}
