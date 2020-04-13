package com.epam.reportportal.testng.integration.feature.step;

import com.epam.reportportal.testng.step.StepReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class ManualStepReporterFeatureTest {

	public static final String FIRST_NAME = "I am the first nested step";
	public static final String SECOND_NAME = "I am the second nested step";

	private static final Logger LOGGER = LoggerFactory.getLogger(ManualStepReporterFeatureTest.class);
	private final StepReporter stepReporter = StepReporter.getInstance();

	@Test
	public void manualStepTest() {

		stepReporter.sendStep(FIRST_NAME);
		LOGGER.info("First info log of the first step");
		LOGGER.info("Second info log of the first step");

		stepReporter.sendStep(SECOND_NAME);
		LOGGER.error("First error log of the second step");

		stepReporter.finishPreviousStep();

		LOGGER.error("Main test method error log");
	}
}
