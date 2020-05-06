/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng;

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Pavel Bortnik
 */
public class Constants {

	static final String BASIC_URL = "basic_url";
	static final String DEFAULT_UUID = "default_uuid";
	static final String DEFAULT_NAME = "default_name";
	static final String DEFAULT_PROJECT = "default_project";
	static final Set<ItemAttributesRQ> ATTRIBUTES = new HashSet<>(Collections.singleton(new ItemAttributesRQ("key", "value")));
	static final Mode MODE = Mode.DEFAULT;
	static final String DEFAULT_DESCRIPTION = "default_description";
	static final Long DEFAULT_TIME = 1515594836210L;

}
