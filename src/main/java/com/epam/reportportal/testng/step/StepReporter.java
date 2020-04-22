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

package com.epam.reportportal.testng.step;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.base.Function;
import rp.com.google.common.collect.Queues;
import rp.com.google.common.collect.Sets;
import rp.com.google.common.io.ByteSource;
import rp.com.google.common.io.ByteStreams;
import rp.com.google.common.io.Files;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporter {

	private final com.epam.reportportal.service.step.StepReporter stepReporter;

	private StepReporter() {
		stepReporter = Launch.currentLaunch().getStepReporter();
	}

	public void sendStep(final String name) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.PASSED, name);
	}

	@Deprecated
	public void sendStep(final String status, final String name) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status), name);
	}

	public void sendStep(@NotNull final ItemStatus status, final String name) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status.name()), name);
	}

	@Deprecated
	public void sendStep(final String status, final String name, final Throwable throwable) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status), name, throwable);
	}

	public void sendStep(final @NotNull ItemStatus status, final String name, final Throwable throwable) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status.name()), name, throwable);
	}

	public void sendStep(final String name, final File... files) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.PASSED, name, files);
	}

	@Deprecated
	public void sendStep(final String status, final String name, final File... files) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status), name, files);
	}

	public void sendStep(final @NotNull ItemStatus status, final String name, final File... files) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status.name()), name, files);
	}

	@Deprecated
	public void sendStep(final String status, String name, final Throwable throwable, @NotNull final File... files) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status), name, throwable, files);
	}

	public void sendStep(final @NotNull ItemStatus status, final String name, final Throwable throwable, final File... files) {
		stepReporter.sendStep(com.epam.reportportal.listeners.ItemStatus.valueOf(status.name()), name, throwable, files);
	}

	public void finishPreviousStep() {
		stepReporter.finishPreviousStep();
	}

	public static StepReporter getInstance() {
		return new StepReporter();
	}
}
