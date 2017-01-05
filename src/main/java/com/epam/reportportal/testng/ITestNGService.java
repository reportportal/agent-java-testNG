/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-testNG
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
 */
package com.epam.reportportal.testng;

import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.Set;

/**
 * Describes all operations for com.epam.reportportal.testng RP listener handler
 * 
 */

public interface ITestNGService {

	/**
	 * Start current launch
	 */
	void startLaunch();

	/**
	 * Finish current launch
	 */
	void finishLaunch();

	/**
	 * Add tags to launch
	 */
	public void addTags(Set<String> tags);

	/**
	 * Start test suite event handler
	 * 
	 * @param suite
	 */
	void startTestSuite(ISuite suite);

	/**
	 * Finish test suite event handler
	 * 
	 * @param suite
	 */
	void finishTestSuite(ISuite suite);

	/**
	 * Start test event handler
	 * 
	 * @param testContext
	 */
	void startTest(ITestContext testContext);

	/**
	 * Finish test event handler
	 * 
	 * @param testContext
	 */
	void finishTest(ITestContext testContext);

	/**
	 * Start test method event handler
	 * 
	 * @param testResult
	 */
	void startTestMethod(ITestResult testResult);

	/**
	 * Finish test method event handler
	 * 
	 * @param status
	 * @param testResult
	 */
	void finishTestMethod(String status, ITestResult testResult);

	/**
	 * Start configuration method(any before of after method)
	 * 
	 * @param testResult
	 */
	void startConfiguration(ITestResult testResult);

	void sendReportPortalMsg(ITestResult testResult);
}
