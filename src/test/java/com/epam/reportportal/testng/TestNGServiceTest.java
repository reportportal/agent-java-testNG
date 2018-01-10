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

import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.mockito.Mockito;
import org.testng.ISuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import rp.com.google.common.base.Supplier;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Pavel Bortnik
 */
public class TestNGServiceTest {

	public static final String RP_ID = "rp_id";

	private TestNGService testNGService;

	private Launch launch;

	private ISuite suite;

	@BeforeClass
	public void init() {
		launch = mock(Launch.class);
		suite = mock(ISuite.class);
		testNGService = new TestNGService(new TestNGService.MemoizingSupplier<Launch>(new Supplier<Launch>() {
			@Override
			public Launch get() {
				return launch;
			}
		}));
	}

	@Test
	public void startLaunch() {
		testNGService.startLaunch();
		verify(launch, times(1)).start();
	}

	@Test
	public void finishLaunch() {
		testNGService.finishLaunch();
		verify(launch, times(1)).finish(Mockito.any(FinishExecutionRQ.class));
	}

	@Test
	public void startTest() {
		testNGService.startTestSuite(suite);
		verify(launch, times(1)).startTestItem(any(StartTestItemRQ.class));
		verify(suite, times(1)).setAttribute(eq(RP_ID), any(Maybe.class));
	}

}
