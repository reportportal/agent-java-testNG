package com.epam.reportportal.testng.util;

import com.epam.reportportal.service.tree.TestItemTree;
import io.reactivex.annotations.Nullable;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeUtils {

	private ItemTreeUtils() {
		//static only
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(ISuite suite, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(suite.getName());
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(ITestContext testContext, TestItemTree testItemTree) {
		TestItemTree.TestItemLeaf suiteLeaf = retrieveLeaf(testContext.getSuite(), testItemTree);
		return suiteLeaf != null ? suiteLeaf.getChildItems().get(testContext.getName()) : null;
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(ITestResult testResult, TestItemTree testItemTree) {
		TestItemTree.TestItemLeaf testLeaf = retrieveLeaf(testResult.getTestContext(), testItemTree);
		return testLeaf != null ? testLeaf.getChildItems().get(testResult.getName()) : null;

	}
}
