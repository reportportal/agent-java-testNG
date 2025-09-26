/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
		Optional<TestItemTree.TestItemLeaf> testClassLeaf = retrieveLeaf(
				testResult.getTestContext(),
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
