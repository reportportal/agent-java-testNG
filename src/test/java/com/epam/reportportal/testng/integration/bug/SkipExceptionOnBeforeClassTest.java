/*
 * Copyright 2023 EPAM Systems
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

package com.epam.reportportal.testng.integration.bug;

import org.testng.SkipException;
import org.testng.annotations.*;

public class SkipExceptionOnBeforeClassTest {
    public static final String SKIP_EXCEPTION = "Just skip the test!";

    @BeforeClass
    public void beforeClass() {
        throw new SkipException(SKIP_EXCEPTION);
    }

    @BeforeMethod
    public void beforeMethod() {
        // should just be skipped
    }

    @Test
    public void theTest() {
        // should just be skipped
    }

    @AfterMethod
    public void afterMethod() {
        // should just be skipped
    }

    @AfterClass
    public void afterClass() {
        // should just be skipped
    }
}
