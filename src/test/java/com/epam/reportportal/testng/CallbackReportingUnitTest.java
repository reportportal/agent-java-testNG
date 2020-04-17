package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.testng.util.ItemTreeUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.util.ArrayList;

import static com.epam.reportportal.testng.TestNGService.ITEM_TREE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class CallbackReportingUnitTest {

	private static final String RP_ID = "rp_id";

	private TestNGService testNGService;

	@Mock
	private Launch launch;

	@Mock
	private ITestContext testContext;

	@Mock
	private ITestResult testResult;

	@Mock
	private ITestNGMethod method;

	@Mock
	private ISuite suite;

	@Mock
	private Maybe<String> id;

	@BeforeEach
	public void preconditions() {
		testNGService = new TestNGService(new TestNGService.MemorizingSupplier<>(() -> launch));
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

		XmlSuite xmlSuiteMock = mock(XmlSuite.class);
		when(suite.getName()).thenReturn(suiteName);
		when(suite.getXmlSuite()).thenReturn(xmlSuiteMock);
		when(xmlSuiteMock.getTests()).thenReturn(new ArrayList<>());

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

		XmlSuite xmlSuiteMock = mock(XmlSuite.class);
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

		XmlSuite xmlSuiteMock = mock(XmlSuite.class);
		when(suite.getName()).thenReturn(suiteName);
		ITEM_TREE.getTestItems().put(ItemTreeUtils.createKey(suite), TestItemTree.createTestItemLeaf(id, 1));

		testNGService.finishTestSuite(suite);
		assertTrue(ITEM_TREE.getTestItems().isEmpty());
	}

}
