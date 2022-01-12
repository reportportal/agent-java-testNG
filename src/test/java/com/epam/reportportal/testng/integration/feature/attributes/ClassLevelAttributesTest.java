package com.epam.reportportal.testng.integration.feature.attributes;

import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

@Attributes(attributes = { @Attribute(key = ClassLevelAttributesTest.KEY, value = ClassLevelAttributesTest.VALUE) })
public class ClassLevelAttributesTest {
	public static final String KEY = "attribute_test_key";
	public static final String VALUE = "attribute_test_value";

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassLevelAttributesTest.class);

	@Test
	public void testClassLevelAttributes() {
		LOGGER.info("Inside 'testClassLevelAttributes' method");
	}

}
