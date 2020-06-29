package com.epam.reportportal.testng;

import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.analytics.AnalyticsService;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsTestNGServiceExtension extends TestNGService {
	public GoogleAnalyticsTestNGServiceExtension(ReportPortal reportPortal, AnalyticsService analyticsService) {
		super(analyticsService);
		setReportPortal(reportPortal);
	}
}
