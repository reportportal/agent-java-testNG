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

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ManualStepReporterSimpleTest {

	public static final String FIRST_NAME = "I am the first nested step";

	private static final Logger LOGGER = LoggerFactory.getLogger(ManualStepReporterSimpleTest.class);

	@Test
	public void manualStepTest() {

		StepReporter stepReporter = Launch.currentLaunch().getStepReporter();

		stepReporter.sendStep(FIRST_NAME);
		LOGGER.info("Inside first nested step");
	}
}
