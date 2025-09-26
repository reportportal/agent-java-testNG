package com.epam.reportportal.testng.util.internal;

import jakarta.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A ConcurrentHashMap with limited size, to act like thread-safe cache.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class LimitedSizeConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
	private final int maxSize;
	private final Queue<K> inputOrder = new ConcurrentLinkedQueue<>();

	public LimitedSizeConcurrentHashMap(final int maximumMapSize) {
		maxSize = maximumMapSize;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(@Nonnull final K key, @Nonnull final V value) {
		if (size() >= maxSize) {
			K keyToRemove = inputOrder.poll();
			if (keyToRemove != null) {
				remove(keyToRemove);
			}
		}
		inputOrder.add(key);
		return super.put(key, value);
	}
}