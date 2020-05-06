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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testng.ITestNGMethod;

import static com.epam.reportportal.testng.TestMethodType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Pavel Bortnik
 */
public class MethodTypeTest {

	private ITestNGMethod testNGMethod;

	@BeforeEach
	public void init() {
		testNGMethod = Mockito.mock(ITestNGMethod.class);
	}

	@Test
	public void testStep() {
		when(testNGMethod.isTest()).thenReturn(true);
		assertEquals(STEP, getStepType(testNGMethod));
	}

	@Test
	public void testAfterClassMethod() {
		when(testNGMethod.isAfterClassConfiguration()).thenReturn(true);
		assertEquals(AFTER_CLASS, getStepType(testNGMethod));
	}

	@Test
	public void testAfterGroups() {
		when(testNGMethod.isAfterGroupsConfiguration()).thenReturn(true);
		assertEquals(AFTER_GROUPS, getStepType(testNGMethod));
	}

	@Test
	public void testAfterMethod() {
		when(testNGMethod.isAfterMethodConfiguration()).thenReturn(true);
		assertEquals(AFTER_METHOD, getStepType(testNGMethod));
	}

	@Test
	public void testAfterSuite() {
		when(testNGMethod.isAfterSuiteConfiguration()).thenReturn(true);
		assertEquals(AFTER_SUITE, getStepType(testNGMethod));
	}

	@Test
	public void testAfterTest() {
		when(testNGMethod.isAfterTestConfiguration()).thenReturn(true);
		assertEquals(AFTER_TEST, getStepType(testNGMethod));
	}

	@Test
	public void testBeforeClass() {
		when(testNGMethod.isBeforeClassConfiguration()).thenReturn(true);
		assertEquals(BEFORE_CLASS, getStepType(testNGMethod));
	}

	@Test
	public void testBeforeGroups() {
		when(testNGMethod.isBeforeGroupsConfiguration()).thenReturn(true);
		assertEquals(BEFORE_GROUPS, getStepType(testNGMethod));
	}

	@Test
	public void testBeforeSuite() {
		when(testNGMethod.isBeforeSuiteConfiguration()).thenReturn(true);
		assertEquals(BEFORE_SUITE, getStepType(testNGMethod));
	}

	@Test
	public void testBeforeMethod() {
		when(testNGMethod.isBeforeMethodConfiguration()).thenReturn(true);
		assertEquals(BEFORE_METHOD, getStepType(testNGMethod));
	}

	@Test
	public void testBeforeTest() {
		when(testNGMethod.isBeforeTestConfiguration()).thenReturn(true);
		assertEquals(BEFORE_TEST, getStepType(testNGMethod));
	}

	@Test
	public void testNull() {
		assertEquals(null, getStepType(testNGMethod));
	}

}
