package com.epam.reportportal.testng.integration;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGService;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ManualStepReportPortalListener extends BaseTestNGListener {

	public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

	public ManualStepReportPortalListener() {
		super(new TestNGService(REPORT_PORTAL_THREAD_LOCAL.get()));
	}

	public static void initReportPortal(ReportPortal reportPortal) {
		REPORT_PORTAL_THREAD_LOCAL.set(reportPortal);
	}
}
