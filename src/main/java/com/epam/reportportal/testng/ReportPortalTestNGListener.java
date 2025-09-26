/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.testng;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.MemoizingSupplier;

import java.util.function.Supplier;

/**
 * Backward-compatible version of Listeners with version prior to 3.0.0
 * Allows to have as many listener instances as needed.
 * The best approach is to have only one instance
 */
public class ReportPortalTestNGListener extends BaseTestNGListener {

	/* static instance with lazy init */
	public static final Supplier<ITestNGService> SERVICE = new MemoizingSupplier<>(() -> new TestNGService(ReportPortal.builder().build()));

	public ReportPortalTestNGListener() {
		super(SERVICE.get());
	}

}
