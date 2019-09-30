package com.epam.reportportal.testng.util;

import com.epam.reportportal.service.tree.TestItemTree;
import io.reactivex.annotations.Nullable;
import org.testng.IClass;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlClass;

import java.util.Arrays;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeUtils {

	private ItemTreeUtils() {
		//static only
	}

	public static TestItemTree.ItemTreeKey createKey(ISuite suite) {
		return TestItemTree.ItemTreeKey.of(suite.getName());
	}

	public static TestItemTree.ItemTreeKey createKey(ITestContext testContext) {
		return TestItemTree.ItemTreeKey.of(testContext.getName());
	}

	public static TestItemTree.ItemTreeKey createKey(XmlClass testClass) {
		return TestItemTree.ItemTreeKey.of(testClass.getName());
	}

	public static TestItemTree.ItemTreeKey createKey(IClass testClass) {
		return TestItemTree.ItemTreeKey.of(testClass.getName());
	}

	public static TestItemTree.ItemTreeKey createKey(ITestResult testResult) {
		return TestItemTree.ItemTreeKey.of(testResult.getName(), Arrays.hashCode(testResult.getParameters()));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(ISuite suite, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createKey(suite));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(ITestContext testContext, TestItemTree testItemTree) {
		TestItemTree.TestItemLeaf suiteLeaf = retrieveLeaf(testContext.getSuite(), testItemTree);
		return suiteLeaf != null ? suiteLeaf.getChildItems().get(createKey(testContext)) : null;
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(ITestResult testResult, TestItemTree testItemTree) {
		TestItemTree.TestItemLeaf testClassLeaf = retrieveLeaf(testResult.getTestContext(), testResult.getTestClass(), testItemTree);
		return testClassLeaf != null ? testClassLeaf.getChildItems().get(createKey(testResult)) : null;
	}

	@Nullable
	private static TestItemTree.TestItemLeaf retrieveLeaf(ITestContext testContext, IClass testClass, TestItemTree testItemTree) {
		TestItemTree.TestItemLeaf testLeaf = retrieveLeaf(testContext, testItemTree);
		return testLeaf != null ? testLeaf.getChildItems().get(createKey(testClass)) : null;
	}
}
