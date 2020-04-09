package com.epam.reportportal.testng.integration;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGService;
import rp.com.google.common.base.Supplier;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class TestReportPortalListener extends BaseTestNGListener {

	static Launch launch;

	public TestReportPortalListener() {
		super(new TestNGService(new Supplier<Launch>() {
			@Override
			public Launch get() {
				return launch;
			}
		}));
	}
}
