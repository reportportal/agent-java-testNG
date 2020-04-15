package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.testng.util.ItemTreeUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.util.ArrayList;

import static com.epam.reportportal.testng.TestNGService.ITEM_TREE;
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

	@Before
	public void preconditions() {
		MockitoAnnotations.initMocks(this);

		testNGService = new TestNGService(new TestNGService.MemoizingSupplier<>(() -> launch));

		when(testResult.getTestContext()).thenReturn(testContext);
		when(testResult.getMethod()).thenReturn(method);
		when(testResult.getAttribute(RP_ID)).thenReturn(id);
		when(method.getRetryAnalyzer(testResult)).thenReturn(result -> false);
		when(testContext.getSuite()).thenReturn(suite);
		when(testContext.getAttribute(RP_ID)).thenReturn(id);

		ITEM_TREE.getTestItems().clear();
	}

	@Test
	public void itemTreeLaunchIdShouldBeInitialized() {
		when(launch.start()).thenReturn(id);

		testNGService.startLaunch();
		Assert.assertNotNull(ITEM_TREE.getLaunchId());
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
		Assert.assertFalse(ITEM_TREE.getTestItems().isEmpty());
		Assert.assertEquals(1, ITEM_TREE.getTestItems().size());
	}

	@Test
	public void treeIsUpdatedIfCallbackIsEnabled1() {
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
		Assert.assertFalse(ITEM_TREE.getTestItems().isEmpty());
		Assert.assertEquals(1, ITEM_TREE.getTestItems().size());
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
		when(suite.getXmlSuite()).thenReturn(xmlSuiteMock);
		when(xmlSuiteMock.getTests()).thenReturn(new ArrayList<>());

		testNGService.startTestSuite(suite);
		Assert.assertTrue(ITEM_TREE.getTestItems().isEmpty());
	}

	@Test
	public void suiteLeafShouldBeRemovedWhenFinishSuite() {
		final String suiteName = "Suite name";

		ListenerParameters listenerParameters = mock(ListenerParameters.class);
		when(launch.getParameters()).thenReturn(listenerParameters);
		when(listenerParameters.isCallbackReportingEnabled()).thenReturn(true);
		when(launch.startTestItem(any(StartTestItemRQ.class))).thenReturn(id);

		XmlSuite xmlSuiteMock = mock(XmlSuite.class);
		when(suite.getName()).thenReturn(suiteName);
		when(suite.getXmlSuite()).thenReturn(xmlSuiteMock);
		when(xmlSuiteMock.getTests()).thenReturn(new ArrayList<>());
		ITEM_TREE.getTestItems().put(ItemTreeUtils.createKey(suite), TestItemTree.createTestItemLeaf(id, 1));

		testNGService.finishTestSuite(suite);
		Assert.assertTrue(ITEM_TREE.getTestItems().isEmpty());
	}

}
