package com.epam.reportportal.testng.integration;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGService;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestReportPortalListener extends BaseTestNGListener {
	public static final ThreadLocal<Launch> LAUNCH_THREAD_LOCAL = new ThreadLocal<>();

	public TestReportPortalListener() {
		super(new TestNGService(LAUNCH_THREAD_LOCAL::get));
	}

	public static void initLaunch(Launch launch) {
		LAUNCH_THREAD_LOCAL.set(launch);
	}

	public static Launch getLaunch() {
		return LAUNCH_THREAD_LOCAL.get();
	}
}
