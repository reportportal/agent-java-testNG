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

import com.epam.ta.reportportal.ws.model.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import rp.com.google.common.collect.ImmutableSet;
import rp.com.google.common.collect.Sets;

import java.util.Set;

/**
 * @author Pavel Bortnik
 */
public class Constants {

	static final String BASIC_URL = "basic_url";
	static final String DEFAULT_UUID = "default_uuid";
	static final String DEFAULT_NAME = "default_name";
	static final String DEFAULT_PROJECT = "default_project";
	static final Set<ItemAttributesRQ> ATTRIBUTES = Sets.newHashSet(new ItemAttributesRQ("key", "value"));
	static final Mode MODE = Mode.DEFAULT;
	static final String DEFAULT_DESCRIPTION = "default_description";
	static final Long DEFAULT_TIME = 1515594836210L;

}
