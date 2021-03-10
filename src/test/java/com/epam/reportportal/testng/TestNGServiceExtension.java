package com.epam.reportportal.testng;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;

import java.util.function.Supplier;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestNGServiceExtension extends TestNGService {

	public TestNGServiceExtension(Supplier<Launch> launch, ReportPortal reportPortal) {
		super(launch);
		setReportPortal(reportPortal);
	}
}
