package com.epam.reportportal.testng.integration.feature.description;

import com.epam.reportportal.annotations.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.NoSuchElementException;

public class DescriptionFailedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionFailedTest.class);

	public static final String TEST_DESCRIPTION = "My test description";
	public static final String ASSERT_ERROR = "Assert Error";
	public static final String NO_SUCH_ELEMENT_EXCEPTION = "No Such Element Exception";

	@Test(description = TEST_DESCRIPTION)
	public void testDescriptionTestAssertError() {
		LOGGER.info("Inside 'testDescriptionTestAssertError' method");
		Assert.fail(ASSERT_ERROR);
	}

	@Test
	public void testWithoutDescriptionTestAssertError() {
		LOGGER.info("Inside 'testWithoutDescriptionTestAssertError' method");
		Assert.fail(ASSERT_ERROR);
	}

	@Test(description = TEST_DESCRIPTION)
	public void testDescriptionTestException() {
		LOGGER.info("Inside 'testDescriptionTestException' method");
		throw new NoSuchElementException(NO_SUCH_ELEMENT_EXCEPTION);
	}

	@Test
	public void testWithoutDescriptionTestException() {
		LOGGER.info("Inside 'testWithoutDescriptionTestException' method");
		throw new NoSuchElementException(NO_SUCH_ELEMENT_EXCEPTION);
	}

	@Test
	@Description(TEST_DESCRIPTION)
	public void testWithDescriptionAnnotationTestException() {
		LOGGER.info("Inside 'testWithoutDescriptionTestException' method");
		throw new NoSuchElementException(NO_SUCH_ELEMENT_EXCEPTION);
	}
}
