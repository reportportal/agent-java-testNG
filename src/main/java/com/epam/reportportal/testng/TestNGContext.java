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

import io.reactivex.Maybe;

/**
 * Context for TestNG Listener
 */
public class TestNGContext {

    private String launchName;

    private Maybe<String> launchID;

    private boolean isLaunchFailed;

    public String getLaunchName() {
        return launchName;
    }

    public void setLaunchName(String launchName) {
        this.launchName = launchName;
    }

    public Maybe<String> getLaunchID() {
        return launchID;
    }

    public void setLaunchID(Maybe<String> launchID) {
        this.launchID = launchID;
    }

    public synchronized boolean getIsLaunchFailed() {
        return isLaunchFailed;
    }

    public synchronized void setIsLaunchFailed(boolean isLaunchFailed) {
        this.isLaunchFailed = isLaunchFailed;
    }
}
