/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng.integration.feature.issue;

import com.epam.reportportal.annotations.Issue;
import org.testng.annotations.Test;

public class SimpleTwoIssuesTest {

	public static final String FAILURE_MESSAGE = "This test is expected to fail";

	@Test
	@Issue(value = "ab001", comment = FAILURE_MESSAGE)
	@Issue(value = "pb001", comment = FAILURE_MESSAGE)
	public void failureTest() {
		throw new IllegalStateException(FAILURE_MESSAGE);
	}
}
