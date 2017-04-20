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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortalClient;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Dzmitry_Kavalets
 */
public class TestNGProvider implements Provider<ITestNGService> {

    private static final ITestNGService NOOP_PROXY = (ITestNGService) Proxy
            .newProxyInstance(TestNGProvider.class.getClassLoader(), new Class[] { ITestNGService.class },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return null;
                        }
                    });

    @Inject
    private ListenerParameters listenerParameters;

    @Inject
    private ReportPortalClient reportPortalClient;

    @Override
    public ITestNGService get() {
        return listenerParameters.getEnable() ? createTestNgService(listenerParameters, reportPortalClient) :
                NOOP_PROXY;
    }

    protected TestNGService createTestNgService(ListenerParameters listenerParameters,
            ReportPortalClient reportPortalClient) {
        return new TestNGService(listenerParameters, reportPortalClient);
    }
}
