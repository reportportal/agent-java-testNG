/*
 * Copyright 2022 EPAM Systems
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

package com.epam.reportportal.testng.integration.feature.testcaseid;

import com.epam.reportportal.annotations.TestCaseId;
import org.testng.annotations.Test;

public class TestCaseIdTemplateTest {

	public static final String FIELD = "template";

	public static final String TEST_CASE_ID_VALUE = "test-case-id-{this.FIELD}";

	@TestCaseId(TEST_CASE_ID_VALUE)
	@Test
	void test() {
	}
}
