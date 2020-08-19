package com.epam.reportportal.testng.integration.feature.nested;

import com.epam.reportportal.annotations.Step;
import org.testng.annotations.Test;

import static com.epam.reportportal.testng.NestedStepTest.INNER_METHOD_NAME_TEMPLATE;
import static com.epam.reportportal.testng.NestedStepTest.METHOD_WITH_INNER_METHOD_NAME_TEMPLATE;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class NestedStepMultiLevelTest {

	@Test
	public void test() {
		methodWithInnerMethod();
	}

	@Step(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE)
	public void methodWithInnerMethod() {
		innerMethod();
	}

	@Step(INNER_METHOD_NAME_TEMPLATE)
	public void innerMethod() {

	}
}
