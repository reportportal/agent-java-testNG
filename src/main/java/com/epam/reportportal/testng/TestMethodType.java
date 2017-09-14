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
