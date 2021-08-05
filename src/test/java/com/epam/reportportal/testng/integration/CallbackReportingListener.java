package com.epam.reportportal.testng.integration;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGServiceExtension;
import com.epam.reportportal.testng.integration.util.TestUtils;
import com.epam.reportportal.utils.MemoizingSupplier;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class CallbackReportingListener extends BaseTestNGListener {

	public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

	public CallbackReportingListener() {
		super(new TestNGServiceExtension(new MemoizingSupplier<>(() -> getLaunch(REPORT_PORTAL_THREAD_LOCAL.get().getParameters())),
				REPORT_PORTAL_THREAD_LOCAL.get()
		));
	}

	public static void initReportPortal(ReportPortal reportPortal) {
		REPORT_PORTAL_THREAD_LOCAL.set(reportPortal);
	}

	public static ReportPortal getReportPortal() {
		return REPORT_PORTAL_THREAD_LOCAL.get();
	}

	private static Launch getLaunch(ListenerParameters parameters) {
		ReportPortal reportPortal = REPORT_PORTAL_THREAD_LOCAL.get();
		return reportPortal.newLaunch(TestUtils.launchRQ(parameters));
	}
}
