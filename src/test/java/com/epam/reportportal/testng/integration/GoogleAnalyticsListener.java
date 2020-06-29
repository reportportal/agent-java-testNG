package com.epam.reportportal.testng.integration;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.analytics.AnalyticsService;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.GoogleAnalyticsTestNGServiceExtension;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsListener extends BaseTestNGListener {

	public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();
	public static final ThreadLocal<AnalyticsService> GOOGLE_ANALYTICS_THREAD_LOCAL = new ThreadLocal<>();

	public GoogleAnalyticsListener() {
		super(new GoogleAnalyticsTestNGServiceExtension(REPORT_PORTAL_THREAD_LOCAL.get(), GOOGLE_ANALYTICS_THREAD_LOCAL.get()));
	}

	public static void initReportPortal(ReportPortal reportPortal) {
		REPORT_PORTAL_THREAD_LOCAL.set(reportPortal);
	}

	public static ReportPortal getReportPortal() {
		return REPORT_PORTAL_THREAD_LOCAL.get();
	}

	public static void initAnalyticsService(AnalyticsService analyticsService) {
		GOOGLE_ANALYTICS_THREAD_LOCAL.set(analyticsService);
	}
}
