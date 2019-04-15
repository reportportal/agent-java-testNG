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

import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import io.reactivex.annotations.Nullable;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import rp.com.google.common.base.Function;

import java.util.Date;

/**
 * Describes all operations for com.epam.reportportal.testng RP listener handler
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
	 * Start test suite event handler
	 *
	 * @param suite TestNG's suite
	 */
	void startTestSuite(ISuite suite);

	/**
	 * Finish test suite event handler
	 *
	 * @param suite TestNG's suite
	 */
	void finishTestSuite(ISuite suite);

	/**
	 * Start test event handler
	 *
	 * @param testContext TestNG's test context
	 */
	void startTest(ITestContext testContext);

	/**
	 * Finish test event handler
	 *
	 * @param testContext TestNG's test context
	 */
	void finishTest(ITestContext testContext);

	/**
	 * Start test method event handler
	 *
	 * @param testResult TestNG's test result
	 */
	void startTestMethod(ITestResult testResult);

	/**
	 * @param uniqueId    {@link com.epam.ta.reportportal.ws.model.StartTestItemRQ#uniqueId}
	 * @param name        {@link com.epam.ta.reportportal.ws.model.StartTestItemRQ#name}
	 * @param description {@link com.epam.ta.reportportal.ws.model.StartTestItemRQ#description}
	 * @param startTime   {@link com.epam.ta.reportportal.ws.model.StartTestItemRQ#startTime}
	 * @param parentId    Nested step's parent ID
	 * @return {@link Maybe} with created step ID
	 */
	Maybe<Long> startNestedStep(@Nullable String uniqueId, String name, String description, Date startTime, Maybe<Long> parentId);

	/**
	 * @param status  {@link com.epam.ta.reportportal.ws.model.FinishTestItemRQ#status}
	 * @param endTime {@link com.epam.ta.reportportal.ws.model.FinishTestItemRQ#endTime}
	 * @param stepId  ID of the nested step to be finished
	 */
	void finishNestedStep(String status, Date endTime, Maybe<Long> stepId);

	void sendReportPortalMsg(Function<Long, SaveLogRQ> saveLogRQFunction);

	/**
	 * Finish test method event handler
	 *
	 * @param status     Status (PASSED/FAILED)
	 * @param testResult TestNG's test result
	 * @see com.epam.reportportal.listeners.Statuses
	 */
	void finishTestMethod(String status, ITestResult testResult);

	/**
	 * Start configuration method(any before of after method)
	 *
	 * @param testResult TestNG's test result
	 */
	void startConfiguration(ITestResult testResult);

	void sendReportPortalMsg(ITestResult testResult);
}
