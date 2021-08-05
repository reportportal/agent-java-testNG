package com.epam.reportportal.testng.integration.feature.attributes;

import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

@Attributes(attributes = { @Attribute(key = "myKey", value = "myValue") })
public class ClassLevelAttributesTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassLevelAttributesTest.class);

	@Test
	public void testClassLevelAttributes() {
		LOGGER.info("Inside 'testClassLevelAttributes' method");
	}

}
