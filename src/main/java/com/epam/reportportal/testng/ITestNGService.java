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

import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

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
