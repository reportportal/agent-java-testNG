/*
 * Copyright (C) 2019 EPAM Systems
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
package com.epam.reportportal.testng;

import rp.com.google.common.base.Supplier;
import rp.com.google.common.base.Suppliers;

/**
 * Backward-compatible version of Listeners with version prior to 3.0.0
 * Allows to have as many listener instances as needed.
 * The best approach is to have only one instance
 */
public class ReportPortalTestNGListener extends BaseTestNGListener {

	/* static instance with lazy init */
	private static final Supplier<ITestNGService> SERVICE = Suppliers.memoize(new Supplier<ITestNGService>() {
		@Override
		public ITestNGService get() {
			return new TestNGService();
		}
	});

	public ReportPortalTestNGListener() {
		super(SERVICE.get());
	}

}
