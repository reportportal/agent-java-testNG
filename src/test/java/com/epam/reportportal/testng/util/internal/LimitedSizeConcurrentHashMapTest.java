package com.epam.reportportal.testng.util.internal;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;

public class LimitedSizeConcurrentHashMapTest {

	@Test
	public void verify_map_size_not_goes_beyond_the_limit() {
		int mapLimit = 3;
		Map<String, String> testMap = new LimitedSizeConcurrentHashMap<>(mapLimit);

		for (int i = 0; i < mapLimit + 1; i++) {
			testMap.put(UUID.randomUUID().toString(), RandomStringUtils.randomAlphanumeric(10));
		}

		assertThat(testMap, aMapWithSize(mapLimit));
	}

}
