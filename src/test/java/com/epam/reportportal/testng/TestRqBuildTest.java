/*
 * Copyright 2017 EPAM Systems
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
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
 *
 */

package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.mockito.Mockito;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import rp.com.google.common.base.Supplier;

import java.util.Calendar;

import static com.epam.reportportal.testng.Constants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Pavel Bortnik
 */
public class TestRqBuildTest {

	private static TestNGService testNGService;

	private static Launch launch;

	@BeforeClass
	public void init() {
		launch = Mockito.mock(Launch.class);
		testNGService = new TestNGService(new TestNGService.MemoizingSupplier<Launch>(new Supplier<Launch>() {
			@Override
			public Launch get() {
				return launch;
			}
		}));
	}

	@Test
	public void testStartLaunchRq() {
		ListenerParameters listenerParameters = defaultListenerParameters();
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(listenerParameters);
		assertThat("Incorrect launch name", startLaunchRQ.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect start time", startLaunchRQ.getStartTime(), notNullValue());
		assertThat("Incorrect launch tags", startLaunchRQ.getTags(), is(TAGS));
		assertThat("Incorrect launch mode", startLaunchRQ.getMode(), is(MODE));
		assertThat("Incorrect description", startLaunchRQ.getDescription(), is(DESCRIPTION));
	}

	@Test
	public void testStartLaunchRq_EmptyDescription() {
		ListenerParameters parameters = new ListenerParameters();
		parameters.setDescription("");
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(parameters);
		assertThat("Description should be null", startLaunchRQ.getDescription(), nullValue());
	}

	@Test
	public void testStartLaunchRq_NullDescription() {
		ListenerParameters parameters = new ListenerParameters();
		StartLaunchRQ startLaunchRQ = testNGService.buildStartLaunchRq(parameters);
		assertThat("Description should be null", startLaunchRQ.getDescription(), nullValue());
	}

	@Test
	public void testStartSuiteRq() {
		ISuite suite = mock(ISuite.class);
		when(suite.getName()).thenReturn(DEFAULT_NAME);

		StartTestItemRQ rq = testNGService.buildStartSuiteRq(suite);

		assertThat("Incorrect suite item type", rq.getType(), is("SUITE"));
		assertThat("Incorrect suite name", rq.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect suite start time", rq.getStartTime(), notNullValue());
	}

	@Test
	public void testStartTestRq() {
		ITestContext testContext = mock(ITestContext.class);
		Calendar instance = Calendar.getInstance();
		instance.setTimeInMillis(DEFAULT_START_TIME);

		when(testContext.getName()).thenReturn(DEFAULT_NAME);
		when(testContext.getStartDate()).thenReturn(instance.getTime());

		StartTestItemRQ rq = testNGService.buildStartTestItemRq(testContext);
		assertThat("Incorrect test item type", rq.getType(), is("TEST"));
		assertThat("Incorrect test item name", rq.getName(), is(DEFAULT_NAME));
		assertThat("Incorrect suite start time", rq.getStartTime(), is(instance.getTime()));
	}

	private ListenerParameters defaultListenerParameters() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl(BASIC_URL);
		listenerParameters.setUuid(DEFAULT_UUID);
		listenerParameters.setLaunchName(DEFAULT_NAME);
		listenerParameters.setProjectName(DEFAULT_PROJECT);
		listenerParameters.setTags(TAGS);
		listenerParameters.setLaunchRunningMode(MODE);
		listenerParameters.setDescription(DESCRIPTION);
		return listenerParameters;
	}

}
