package com.epam.reportportal.testng.util;

import com.epam.reportportal.service.tree.TestItemTree;
import org.testng.IClass;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlClass;

import java.util.Arrays;
import java.util.Optional;

import static java.util.Optional.ofNullable;

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

	public static Optional<TestItemTree.TestItemLeaf> retrieveLeaf(ISuite suite, TestItemTree testItemTree) {
		return ofNullable(testItemTree.getTestItems().get(createKey(suite)));
	}

	public static Optional<TestItemTree.TestItemLeaf> retrieveLeaf(ITestContext testContext, TestItemTree testItemTree) {
		Optional<TestItemTree.TestItemLeaf> suiteLeaf = retrieveLeaf(testContext.getSuite(), testItemTree);
		return suiteLeaf.map(leaf -> leaf.getChildItems().get(createKey(testContext)));
	}

	public static Optional<TestItemTree.TestItemLeaf> retrieveLeaf(ITestResult testResult, TestItemTree testItemTree) {
		Optional<TestItemTree.TestItemLeaf> testClassLeaf = retrieveLeaf(testResult.getTestContext(),
				testResult.getTestClass(),
				testItemTree
		);
		return testClassLeaf.map(leaf -> leaf.getChildItems().get(createKey(testResult)));
	}

	private static Optional<TestItemTree.TestItemLeaf> retrieveLeaf(ITestContext testContext, IClass testClass, TestItemTree testItemTree) {
		Optional<TestItemTree.TestItemLeaf> testLeaf = retrieveLeaf(testContext, testItemTree);
		return testLeaf.map(leaf -> leaf.getChildItems().get(createKey(testClass)));
	}
}
