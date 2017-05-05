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

import rp.com.google.inject.AbstractModule;
import rp.com.google.inject.Scopes;

/**
 * Guice module with TestNG beans.
 *
 * @author Ilya_Koshaleu
 * @author Andrei Varabyeu
 */
public class TestNGAgentModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ITestNGService.class).toProvider(TestNGProvider.class).in(Scopes.SINGLETON);
    }
}
