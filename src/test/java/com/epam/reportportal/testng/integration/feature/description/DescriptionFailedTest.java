package com.epam.reportportal.testng.integration.feature.description;

import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DescriptionFailedTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionFailedTest.class);

  public static final String TEST_DESCRIPTION = "My test description";

  @Test(description = TEST_DESCRIPTION)
  public void testDescriptionTestAssertError() {
    LOGGER.info("Inside 'testDescriptionTestAssertError' method");
    Assert.fail("Assert Error");
  }

  @Test
  public void testWithoutDescriptionTestAssertError() {
    LOGGER.info("Inside 'testWithoutDescriptionTestAssertError' method");
    Assert.fail("Assert Error");
  }

  @Test(description = TEST_DESCRIPTION)
  public void testDescriptionTestException() {
    LOGGER.info("Inside 'testDescriptionTestException' method");
    throw new NoSuchElementException("No Such Element Exception");
  }

  @Test
  public void testWithoutDescriptionTestException() {
    LOGGER.info("Inside 'testWithoutDescriptionTestException' method");
    throw new NoSuchElementException("No Such Element Exception");
  }
}
