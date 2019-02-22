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

import org.testng.ITestNGMethod;

/**
 * Possible types of Test method
 */
public enum TestMethodType {

    //@formatter:off
    STEP,
    BEFORE_CLASS,
    BEFORE_GROUPS,
    BEFORE_METHOD,
    BEFORE_SUITE,
    BEFORE_TEST,
    AFTER_CLASS,
    AFTER_GROUPS,
    AFTER_METHOD,
    AFTER_SUITE,
    AFTER_TEST;
    //@formatter:on

    /**
     * Return method type basing on ITestNGMethod object
     *
     * @param method Method to find type of
     * @return Type of method
     */
    public static TestMethodType getStepType(ITestNGMethod method) {
        if (method.isTest()) {
            return STEP;
        } else if (method.isAfterClassConfiguration()) {
            return AFTER_CLASS;
        } else if (method.isAfterGroupsConfiguration()) {
            return AFTER_GROUPS;
        } else if (method.isAfterMethodConfiguration()) {
            return AFTER_METHOD;
        } else if (method.isAfterSuiteConfiguration()) {
            return AFTER_SUITE;
        } else if (method.isAfterTestConfiguration()) {
            return AFTER_TEST;
        } else if (method.isBeforeClassConfiguration()) {
            return BEFORE_CLASS;
        } else if (method.isBeforeGroupsConfiguration()) {
            return BEFORE_GROUPS;
        } else if (method.isBeforeMethodConfiguration()) {
            return BEFORE_METHOD;
        } else if (method.isBeforeSuiteConfiguration()) {
            return BEFORE_SUITE;
        } else if (method.isBeforeTestConfiguration()) {
            return TestMethodType.BEFORE_TEST;
        } else {
            return null;
        }
    }
}
