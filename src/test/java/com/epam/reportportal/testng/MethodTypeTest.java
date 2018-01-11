/*
 * Copyright 2017 EPAM Systems
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.epam.reportportal.testng;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testng.ITestNGMethod;

import static com.epam.reportportal.testng.TestMethodType.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Pavel Bortnik
 */
public class MethodTypeTest {

	private ITestNGMethod testNGMethod;

	@Before
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
		when(testNGMethod).then(Answers.RETURNS_DEFAULTS);
		assertEquals(null, getStepType(testNGMethod));
	}

}
