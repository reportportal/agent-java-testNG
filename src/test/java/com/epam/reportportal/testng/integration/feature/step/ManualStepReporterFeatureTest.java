/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng.integration.feature.step;

import com.epam.reportportal.testng.ReportPortalTestNGListener;
import com.epam.reportportal.testng.step.ItemStatus;
import com.epam.reportportal.testng.step.StepReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class ManualStepReporterFeatureTest {

	public static final String FIRST_NAME = "I am the first nested step";
	public static final String SECOND_NAME = "I am the second nested step";
	public static final String THIRD_NAME = "I am the third nested step";

	private static final Logger LOGGER = LoggerFactory.getLogger(ManualStepReporterFeatureTest.class);
	private final StepReporter stepReporter = StepReporter.getInstance();

	@Test
	public void manualStepTest() {

		stepReporter.sendStep(FIRST_NAME);
		LOGGER.info("First info log of the first step");
		LOGGER.info("Second info log of the first step");

		stepReporter.sendStep(SECOND_NAME);
		LOGGER.error("First error log of the second step");

		stepReporter.sendStep(ItemStatus.FAILED, THIRD_NAME, new File("pug/unlucky.jpg"));
		LOGGER.error("Second error log of the second step");

		stepReporter.finishPreviousStep();

		LOGGER.error("Main test method error log");
	}
}
