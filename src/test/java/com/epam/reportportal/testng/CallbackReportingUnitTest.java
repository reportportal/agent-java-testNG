package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.testng.util.ItemTreeUtils;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testng.ISuite;


import static com.epam.reportportal.testng.TestNGService.ITEM_TREE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class CallbackReportingUnitTest {

	private TestNGService testNGService;

	@Mock
	private Launch launch;

	@Mock
	private ISuite suite;

	@Mock
	private Maybe<String> id;

	@BeforeEach
	public void preconditions() {
		testNGService = new TestNGService(new MemoizingSupplier<>(() -> launch));
		ITEM_TREE.getTestItems().clear();
	}

	@Test
	public void itemTreeLaunchIdShouldBeInitialized() {
		when(launch.start()).thenReturn(id);

		testNGService.startLaunch();
		assertNotNull(ITEM_TREE.getLaunchId());
	}

	@Test
	public void treeIsUpdatedIfCallbackIsEnabled() {
		final String suiteName = "Suite name";

		ListenerParameters listenerParameters = mock(ListenerParameters.class);
		when(launch.getParameters()).thenReturn(listenerParameters);
		when(listenerParameters.isCallbackReportingEnabled()).thenReturn(true);
		when(launch.startTestItem(any(StartTestItemRQ.class))).thenReturn(id);
		when(suite.getName()).thenReturn(suiteName);

		testNGService.startTestSuite(suite);
		assertFalse(ITEM_TREE.getTestItems().isEmpty());
		assertEquals(1, ITEM_TREE.getTestItems().size());
	}

	@Test
	public void treeIsNotUpdatedIfCallbackIsDisabled() {
		final String suiteName = "Suite name";

		ListenerParameters listenerParameters = mock(ListenerParameters.class);
		when(launch.getParameters()).thenReturn(listenerParameters);
		when(listenerParameters.isCallbackReportingEnabled()).thenReturn(false);
		when(launch.startTestItem(any(StartTestItemRQ.class))).thenReturn(id);
		when(suite.getName()).thenReturn(suiteName);

		testNGService.startTestSuite(suite);
		assertTrue(ITEM_TREE.getTestItems().isEmpty());
	}

	@Test
	public void suiteLeafShouldBeRemovedWhenFinishSuite() {
		final String suiteName = "Suite name";

		ListenerParameters listenerParameters = mock(ListenerParameters.class);
		when(launch.getParameters()).thenReturn(listenerParameters);
		when(listenerParameters.isCallbackReportingEnabled()).thenReturn(true);
		when(suite.getName()).thenReturn(suiteName);
		ITEM_TREE.getTestItems().put(ItemTreeUtils.createKey(suite), TestItemTree.createTestItemLeaf(id));

		testNGService.finishTestSuite(suite);
		assertTrue(ITEM_TREE.getTestItems().isEmpty());
	}

}
