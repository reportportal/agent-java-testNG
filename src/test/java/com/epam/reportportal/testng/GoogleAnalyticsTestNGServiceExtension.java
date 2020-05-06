package com.epam.reportportal.testng;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.analytics.GoogleAnalytics;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsTestNGServiceExtension extends TestNGService {

	private final GoogleAnalytics googleAnalytics;

	public GoogleAnalyticsTestNGServiceExtension(ReportPortal reportPortal, GoogleAnalytics googleAnalytics) {
		super();
		this.googleAnalytics = googleAnalytics;
		setReportPortal(reportPortal);
	}

	@Override
	protected GoogleAnalytics getGoogleAnalytics() {
		return googleAnalytics;
	}
}
