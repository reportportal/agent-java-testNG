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
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.ReportPortal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.*;
import org.testng.internal.IResultListener2;
import rp.com.google.common.base.Supplier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static rp.com.google.common.base.Suppliers.memoize;

/**
 * Report portal custom event listener. Support parallel execution of test
 * methods, suites, test classes.
 * Can be extended by providing {@link ITestNGService} implementation
 */
public class BaseTestNGListener implements IExecutionListener, ISuiteListener, IResultListener2 {

	private static final ITestNGService NOOP_PROXY = (ITestNGService) Proxy.newProxyInstance(BaseTestNGListener.class.getClassLoader(),
			new Class[] { ITestNGService.class },
			new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return null;
				}
			}
	);

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseTestNGListener.class);

	private static final AtomicInteger INSTANCES = new AtomicInteger(0);

	private Supplier<ITestNGService> testNGService;

	public BaseTestNGListener() {
		this(ReportPortal.builder().build());
	}

	public BaseTestNGListener(ListenerParameters parameters) {
		this(ReportPortal.builder().withParameters(parameters).build());
	}

	public BaseTestNGListener(final ReportPortal reportPortal) {
		this(new Supplier<ITestNGService>() {
			@Override
			public ITestNGService get() {
				return reportPortal.getParameters().getEnable() ? new TestNGService(reportPortal) : NOOP_PROXY;
			}
		});
	}

	public BaseTestNGListener(final Supplier<ITestNGService> testNgService) {
		testNGService = memoize(testNgService);
		if (INSTANCES.incrementAndGet() > 1) {
			final String warning = "WARNING! More than one ReportPortal listener is added";
			LOGGER.warn(warning);

			//even if logger is not configured, print the message to default stdout
			System.out.println(warning);
		}
	}

	@Override
	public void onExecutionStart() {
		testNGService.get().startLaunch();
	}

	@Override
	public void onExecutionFinish() {
		testNGService.get().finishLaunch();
	}

	@Override
	public void onStart(ISuite suite) {
		testNGService.get().startTestSuite(suite);
	}

	@Override
	public void onFinish(ISuite suite) {
		testNGService.get().finishTestSuite(suite);
	}

	@Override
	public void onStart(ITestContext testContext) {
		testNGService.get().startTest(testContext);
	}

	@Override
	public void onFinish(ITestContext testContext) {
		testNGService.get().finishTest(testContext);
	}

	@Override
	public void onTestStart(ITestResult testResult) {
		testNGService.get().startTestMethod(testResult);
	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		testNGService.get().finishTestMethod(Statuses.PASSED, testResult);
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		testNGService.get().sendReportPortalMsg(testResult);
		testNGService.get().finishTestMethod(Statuses.FAILED, testResult);
	}

	@Override
	public void onTestSkipped(ITestResult testResult) {
		testNGService.get().startTestMethod(testResult);
		testNGService.get().finishTestMethod(Statuses.SKIPPED, testResult);
	}

	@Override
	public void beforeConfiguration(ITestResult testResult) {
		testNGService.get().startConfiguration(testResult);
	}

	@Override
	public void onConfigurationFailure(ITestResult testResult) {
		testNGService.get().sendReportPortalMsg(testResult);
		testNGService.get().finishTestMethod(Statuses.FAILED, testResult);
	}

	@Override
	public void onConfigurationSuccess(ITestResult testResult) {
		testNGService.get().finishTestMethod(Statuses.PASSED, testResult);
	}

	@Override
	public void onConfigurationSkip(ITestResult testResult) {
		testNGService.get().startConfiguration(testResult);
		testNGService.get().finishTestMethod(Statuses.SKIPPED, testResult);
	}

	// this action temporary doesn't supported by report portal
	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		testNGService.get().finishTestMethod(Statuses.FAILED, result);
	}
}
