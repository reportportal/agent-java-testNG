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

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.aspect.StepAspect;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import io.reactivex.annotations.Nullable;
import org.testng.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;
import rp.com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.epam.reportportal.testng.util.ItemTreeUtils.createKey;
import static java.util.Optional.ofNullable;
import static org.testng.ITestResult.FAILURE;
import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * TestNG service implements operations for interaction report portal
 */
public class TestNGService implements ITestNGService {

	private static final String AGENT_PROPERTIES_FILE = "agent.properties";
	public static final String NOT_ISSUE = "NOT_ISSUE";
	public static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	public static final String RP_ID = "rp_id";
	public static final String ARGUMENT = "arg";

	private static ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	public static final TestItemTree ITEM_TREE = new TestItemTree();

	private final AtomicBoolean isLaunchFailed = new AtomicBoolean();

	private final MemorizingSupplier<Launch> launch;

	public TestNGService() {
		this.launch = new MemorizingSupplier<>(() -> {
			//this reads property, so we want to
			//init ReportPortal object each time Launch object is going to be created

			StartLaunchRQ rq = buildStartLaunchRq(REPORT_PORTAL.getParameters());
			rq.setStartTime(Calendar.getInstance().getTime());
			return REPORT_PORTAL.newLaunch(rq);
		});
	}

	public TestNGService(Supplier<Launch> launch) {
		this.launch = new MemorizingSupplier<>(launch);
	}

	public static ReportPortal getReportPortal() {
		return REPORT_PORTAL;
	}

	protected static void setReportPortal(ReportPortal reportPortal) {
		REPORT_PORTAL = reportPortal;
	}

	@Override
	public void startLaunch() {
		Maybe<String> launchId = this.launch.get().start();
		StepAspect.addLaunch("default", this.launch.get());
		ITEM_TREE.setLaunchId(launchId);
	}

	@Override
	public void finishLaunch() {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(isLaunchFailed.get() ? Statuses.FAILED : Statuses.PASSED);
		launch.get().finish(rq);
		this.launch.reset();
	}

	@Override
	public void startTestSuite(ISuite suite) {
		StartTestItemRQ rq = buildStartSuiteRq(suite);
		final Maybe<String> item = launch.get().startTestItem(rq);
		if (launch.get().getParameters().isCallbackReportingEnabled()) {
			addToTree(suite, item);
		}
		suite.setAttribute(RP_ID, item);
		StepAspect.setParentId(item);
	}

	private void addToTree(ISuite suite, Maybe<String> item) {
		ITEM_TREE.getTestItems().put(createKey(suite), TestItemTree.createTestItemLeaf(item, suite.getXmlSuite().getTests().size()));
	}

	@Override
	public void finishTestSuite(ISuite suite) {
		if (null != suite.getAttribute(RP_ID)) {
			FinishTestItemRQ rq = buildFinishTestSuiteRq(suite);
			launch.get().finishTestItem(this.getAttribute(suite, RP_ID), rq);
			suite.removeAttribute(RP_ID);
		}
		if (launch.get().getParameters().isCallbackReportingEnabled()) {
			removeFromTree(suite);
		}
	}

	private void removeFromTree(ISuite suite) {
		ITEM_TREE.getTestItems().remove(createKey(suite));
	}

	@Override
	public void startTest(ITestContext testContext) {
		if (hasMethodsToRun(testContext)) {
			StartTestItemRQ rq = buildStartTestItemRq(testContext);
			final Maybe<String> testID = launch.get().startTestItem(this.getAttribute(testContext.getSuite(), RP_ID), rq);
			if (launch.get().getParameters().isCallbackReportingEnabled()) {
				addToTree(testContext, testID);
			}
			testContext.setAttribute(RP_ID, testID);
			StepAspect.setParentId(testID);
		}
	}

	private void addToTree(ITestContext testContext, Maybe<String> testId) {
		ofNullable(ITEM_TREE.getTestItems().get(createKey(testContext.getSuite()))).ifPresent(suiteLeaf -> {
			List<XmlClass> testClasses = testContext.getCurrentXmlTest().getClasses();
			ConcurrentHashMap<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> testClassesMapping = new ConcurrentHashMap<>(testClasses.size());
			for (XmlClass testClass : testClasses) {
				TestItemTree.TestItemLeaf testClassLeaf = TestItemTree.createTestItemLeaf(testId, new ConcurrentHashMap<>());
				testClassesMapping.put(createKey(testClass), testClassLeaf);
			}
			suiteLeaf.getChildItems().put(createKey(testContext), TestItemTree.createTestItemLeaf(testId, testClassesMapping));
		});
	}

	@Override
	public void finishTest(ITestContext testContext) {
		if (hasMethodsToRun(testContext)) {
			FinishTestItemRQ rq = buildFinishTestRq(testContext);
			launch.get().finishTestItem(this.getAttribute(testContext, RP_ID), rq);
			if (launch.get().getParameters().isCallbackReportingEnabled()) {
				removeFromTree(testContext);
			}
		}
	}

	private void removeFromTree(ITestContext testContext) {
		ofNullable(ITEM_TREE.getTestItems().get(createKey(testContext.getSuite()))).ifPresent(suiteLeaf -> suiteLeaf.getChildItems()
				.remove(createKey(testContext)));
	}

	@Override
	public void startTestMethod(ITestResult testResult) {
		StartTestItemRQ rq = buildStartStepRq(testResult);
		if (rq == null) {
			return;
		}
		Maybe<String> stepMaybe = launch.get().startTestItem(this.getAttribute(testResult.getTestContext(), RP_ID), rq);
		testResult.setAttribute(RP_ID, stepMaybe);
		StepAspect.setParentId(stepMaybe);
		if (launch.get().getParameters().isCallbackReportingEnabled()) {
			addToTree(testResult, stepMaybe);
		}
	}

	private void addToTree(ITestResult testResult, Maybe<String> stepMaybe) {
		ITestContext testContext = testResult.getTestContext();

		ofNullable(ITEM_TREE.getTestItems()
				.get(createKey(testContext.getSuite()))).flatMap(suiteLeaf -> ofNullable(suiteLeaf.getChildItems()
				.get(createKey(testContext))).flatMap(testLeaf -> ofNullable(testLeaf.getChildItems()
				.get(createKey(testResult.getTestClass())))))
				.ifPresent(testClassLeaf -> testClassLeaf.getChildItems()
						.put(createKey(testResult), TestItemTree.createTestItemLeaf(stepMaybe, 0)));
	}

	@Override
	public void finishTestMethod(String status, ITestResult testResult) {
		if (Statuses.SKIPPED.equals(status) && !isRetry(testResult) && null == testResult.getAttribute(RP_ID)) {
			startTestMethod(testResult);
		}

		Maybe<String> itemId = getAttribute(testResult, RP_ID);
		if (launch.get().getStepReporter().isParentFailed(itemId)) {
			status = Statuses.FAILED;
			testResult.setStatus(FAILURE);
		}
		FinishTestItemRQ rq = buildFinishTestMethodRq(status, testResult);
		Maybe<OperationCompletionRS> finishItemResponse = launch.get().finishTestItem(this.getAttribute(testResult, RP_ID), rq);
		if (launch.get().getParameters().isCallbackReportingEnabled()) {
			updateTestItemTree(finishItemResponse, testResult);
		}
	}

	private void updateTestItemTree(Maybe<OperationCompletionRS> finishItemResponse, ITestResult testResult) {
		ITestContext testContext = testResult.getTestContext();
		TestItemTree.TestItemLeaf suiteLeaf = ITEM_TREE.getTestItems().get(createKey(testContext.getSuite()));
		if (suiteLeaf != null) {
			TestItemTree.TestItemLeaf testLeaf = suiteLeaf.getChildItems().get(createKey(testContext));
			if (testLeaf != null) {
				TestItemTree.TestItemLeaf testClassLeaf = testLeaf.getChildItems().get(createKey(testResult.getTestClass()));
				if (testClassLeaf != null) {
					TestItemTree.TestItemLeaf testItemLeaf = testClassLeaf.getChildItems().get(createKey(testResult));
					if (testItemLeaf != null) {
						testItemLeaf.setFinishResponse(finishItemResponse);
					}
				}
			}
		}
	}

	@Override
	public void startConfiguration(ITestResult testResult) {
		TestMethodType type = TestMethodType.getStepType(testResult.getMethod());
		StartTestItemRQ rq = buildStartConfigurationRq(testResult, type);

		Maybe<String> parentId = getConfigParent(testResult, type);
		final Maybe<String> itemID = launch.get().startTestItem(parentId, rq);
		testResult.setAttribute(RP_ID, itemID);
		StepAspect.setParentId(itemID);
	}

	@Override
	public void sendReportPortalMsg(final ITestResult result) {
		ReportPortal.emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel("ERROR");
			rq.setLogTime(Calendar.getInstance().getTime());
			if (result.getThrowable() != null) {
				rq.setMessage(getStackTraceAsString(result.getThrowable()));
			} else {
				rq.setMessage("Test has failed without exception");
			}
			rq.setLogTime(Calendar.getInstance().getTime());

			return rq;
		});
	}

	/**
	 * Extension point to customize suite creation event/request
	 *
	 * @param suite TestNG suite
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartSuiteRq(ISuite suite) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(suite.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("SUITE");
		return rq;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param testContext TestNG test context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartTestItemRq(ITestContext testContext) {
		StartTestItemRQ rq = new StartTestItemRQ();
		XmlTest currentXmlTest = testContext.getCurrentXmlTest();
		if (currentXmlTest != null) {
			List<XmlClass> xmlClasses = currentXmlTest.getXmlClasses();
			if (xmlClasses != null) {
				XmlClass xmlClass = xmlClasses.get(0);
				if (xmlClass != null) {
					rq.setCodeRef(xmlClass.getName());
				}
			}
		}
		rq.setName(testContext.getName());
		rq.setStartTime(testContext.getStartDate());
		rq.setType("TEST");
		return rq;
	}

	/**
	 * Extension point to customize launch creation event/request
	 *
	 * @param parameters Launch Configuration parameters
	 * @return Request to ReportPortal
	 */
	protected StartLaunchRQ buildStartLaunchRq(ListenerParameters parameters) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(parameters.getLaunchName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setAttributes(parameters.getAttributes());
		rq.setMode(parameters.getLaunchRunningMode());
		rq.setRerun(parameters.isRerun());
		if (!isNullOrEmpty(parameters.getRerunOf())) {
			rq.setRerunOf(parameters.getRerunOf());
		}
		if (!isNullOrEmpty(parameters.getDescription())) {
			rq.setDescription(parameters.getDescription());
		}
		if (null != parameters.getSkippedAnIssue()) {
			ItemAttributesRQ skippedIssueAttribute = new ItemAttributesRQ();
			skippedIssueAttribute.setKey(SKIPPED_ISSUE_KEY);
			skippedIssueAttribute.setValue(parameters.getSkippedAnIssue().toString());
			skippedIssueAttribute.setSystem(true);
			rq.getAttributes().add(skippedIssueAttribute);
		}
		rq.getAttributes().addAll(SystemAttributesExtractor.extract(AGENT_PROPERTIES_FILE, TestNGService.class.getClassLoader()));
		return rq;
	}

	/**
	 * Extension point to customize beforeXXX creation event/request
	 *
	 * @param testResult TestNG's testResult context
	 * @param type       Type of method
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartConfigurationRq(ITestResult testResult, TestMethodType type) {
		StartTestItemRQ rq = new StartTestItemRQ();
		String configName = testResult.getMethod().getMethodName();
		rq.setName(configName);

		rq.setDescription(testResult.getMethod().getDescription());
		rq.setStartTime(new Date(testResult.getStartMillis()));
		rq.setType(type == null ? null : type.toString());
		return rq;
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param testResult TestNG's testResult context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(ITestResult testResult) {
		//		if (testResult.getAttribute(RP_ID) != null) {
		//			return null;
		//		}
		StartTestItemRQ rq = new StartTestItemRQ();
		String testStepName;
		if (testResult.getTestName() != null) {
			testStepName = testResult.getTestName();
		} else {
			testStepName = testResult.getMethod().getMethodName();
		}
		rq.setName(testStepName);
		String codeRef = testResult.getMethod().getQualifiedName();
		rq.setCodeRef(codeRef);
		rq.setTestCaseId(getTestCaseId(codeRef, testResult).getId());
		rq.setAttributes(createStepAttributes(testResult));
		rq.setDescription(createStepDescription(testResult));
		rq.setParameters(createStepParameters(testResult));
		rq.setUniqueId(extractUniqueID(testResult));
		rq.setStartTime(new Date(testResult.getStartMillis()));
		rq.setType(TestMethodType.getStepType(testResult.getMethod()).toString());

		rq.setRetry(isRetry(testResult));
		return rq;
	}

	/**
	 * Extension point to customize test suite on it's finish
	 *
	 * @param suite TestNG's suite context
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishTestSuiteRq(ISuite suite) {
		/* 'real' end time */
		Date now = Calendar.getInstance().getTime();
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(now);
		rq.setStatus(getSuiteStatus(suite));
		return rq;
	}

	/**
	 * Extension point to customize test on it's finish
	 *
	 * @param testContext TestNG test context
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishTestRq(ITestContext testContext) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(testContext.getEndDate());
		String status = isTestPassed(testContext) ? Statuses.PASSED : Statuses.FAILED;
		rq.setStatus(status);
		return rq;
	}

	/**
	 * Extension point to customize test method on it's finish
	 *
	 * @param testResult TestNG's testResult context
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishTestMethodRq(String status, ITestResult testResult) {
		return buildFinishTestMethodRq(status, new Date(testResult.getEndMillis()));
	}

	private FinishTestItemRQ buildFinishTestMethodRq(String status, Date endTime) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(endTime);
		rq.setStatus(status);
		// Allows indicate that SKIPPED is not to investigate items for WS
		if (Statuses.SKIPPED.equals(status) && !ofNullable(launch.get().getParameters().getSkippedAnIssue()).orElse(false)) {
			Issue issue = new Issue();
			issue.setIssueType(NOT_ISSUE);
			rq.setIssue(issue);
		}
		return rq;
	}

	/**
	 * Extension point to customize Report Portal test parameters
	 *
	 * @param testResult TestNG's testResult context
	 * @return Test/Step Parameters being sent to Report Portal
	 */
	protected List<ParameterResource> createStepParameters(ITestResult testResult) {
		List<ParameterResource> parameters = Lists.newArrayList();
		Test testAnnotation = getMethodAnnotation(Test.class, testResult);
		Parameters parametersAnnotation = getMethodAnnotation(Parameters.class, testResult);
		if (null != testAnnotation && !isNullOrEmpty(testAnnotation.dataProvider())) {
			parameters = createDataProviderParameters(testResult);
		} else if (null != parametersAnnotation) {
			parameters = createAnnotationParameters(testResult, parametersAnnotation);
		}
		return parameters.isEmpty() ? null : parameters;
	}

	/**
	 * Process testResult to create parameters provided via {@link Parameters}
	 *
	 * @param testResult           TestNG's testResult context
	 * @param parametersAnnotation Annotation with parameters
	 * @return Step Parameters being sent to Report Portal
	 */
	private List<ParameterResource> createAnnotationParameters(ITestResult testResult, Parameters parametersAnnotation) {
		List<ParameterResource> params = Lists.newArrayList();
		String[] keys = parametersAnnotation.value();
		Object[] parameters = testResult.getParameters();
		if (parameters.length != keys.length) {
			return params;
		}
		for (int i = 0; i < keys.length; i++) {
			ParameterResource parameter = new ParameterResource();
			parameter.setKey(keys[i]);
			parameter.setValue(parameters[i] != null ? parameters[i].toString() : "");
			params.add(parameter);
		}
		return params;
	}

	/**
	 * Processes testResult to create parameters provided
	 * by {@link org.testng.annotations.DataProvider} If parameter key isn't provided
	 * by {@link ParameterKey} annotation then it will be 'arg[index]'
	 *
	 * @param testResult TestNG's testResult context
	 * @return Step Parameters being sent to ReportPortal
	 */

	private List<ParameterResource> createDataProviderParameters(ITestResult testResult) {
		List<ParameterResource> result = Lists.newArrayList();
		Annotation[][] parameterAnnotations = testResult.getMethod().getConstructorOrMethod().getMethod().getParameterAnnotations();
		Object[] values = testResult.getParameters();
		int length = parameterAnnotations.length;
		if (length != values.length) {
			return result;
		}
		for (int i = 0; i < length; i++) {
			ParameterResource parameter = new ParameterResource();
			String key = ARGUMENT + i;
			String value = values[i] != null ? values[i].toString() : null;
			if (parameterAnnotations[i].length > 0) {
				for (int j = 0; j < parameterAnnotations[i].length; j++) {
					Annotation annotation = parameterAnnotations[i][j];
					if (annotation.annotationType().equals(ParameterKey.class)) {
						key = ((ParameterKey) annotation).value();
					}
				}
			}
			parameter.setKey(key);
			parameter.setValue(value != null ? value : "");
			result.add(parameter);
		}
		return result;
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param testResult TestNG's testResult context
	 * @return Test/Step Description being sent to ReportPortal
	 */
	protected String createStepDescription(ITestResult testResult) {
		StringBuilder stringBuffer = new StringBuilder();
		if (testResult.getMethod().getDescription() != null) {
			stringBuffer.append(testResult.getMethod().getDescription());
		}
		return stringBuffer.toString();
	}

	/**
	 * Extension point to customize test suite status being sent to ReportPortal
	 *
	 * @param suite TestNG's suite
	 * @return Status PASSED/FAILED/etc
	 */
	protected String getSuiteStatus(ISuite suite) {
		Collection<ISuiteResult> suiteResults = suite.getResults().values();
		String suiteStatus = Statuses.PASSED;
		for (ISuiteResult suiteResult : suiteResults) {
			if (!(isTestPassed(suiteResult.getTestContext()))) {
				suiteStatus = Statuses.FAILED;
				break;
			}
		}
		// if at least one suite failed launch should be failed
		isLaunchFailed.compareAndSet(false, suiteStatus.equals(Statuses.FAILED));
		return suiteStatus;
	}

	/**
	 * Check is current method passed according the number of failed tests and
	 * configurations
	 *
	 * @param testContext TestNG's test content
	 * @return TRUE if passed, FALSE otherwise
	 */
	protected boolean isTestPassed(ITestContext testContext) {
		return testContext.getFailedTests().size() == 0 && testContext.getFailedConfigurations().size() == 0
				&& testContext.getSkippedConfigurations().size() == 0 && testContext.getSkippedTests().size() == 0;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getAttribute(IAttributes attributes, String attribute) {
		return (T) attributes.getAttribute(attribute);
	}

	/**
	 * Returns test item ID from annotation if it provided.
	 *
	 * @param testResult Where to find
	 * @return test item ID or null
	 */
	private String extractUniqueID(ITestResult testResult) {
		UniqueID itemUniqueID = getMethodAnnotation(UniqueID.class, testResult);
		return itemUniqueID != null ? itemUniqueID.value() : null;
	}

	@Nullable
	private TestCaseIdEntry getTestCaseId(String codeRef, ITestResult testResult) {
		TestCaseId testCaseId = getMethodAnnotation(TestCaseId.class, testResult);
		return testCaseId != null ?
				getTestCaseId(testCaseId, testResult) :
				new TestCaseIdEntry(testCaseIdFromCodeRefAndParams(codeRef, testResult.getParameters()));
	}

	private String testCaseIdFromCodeRefAndParams(String codeRef, Object[] parameters) {
		boolean isParametersPresent = Objects.nonNull(parameters) && parameters.length > 0;
		return isParametersPresent ? codeRef + TRANSFORM_PARAMETERS.apply(parameters) : codeRef;
	}

	private static final Function<Object[], String> TRANSFORM_PARAMETERS = it -> "[" + Arrays.stream(it)
			.map(String::valueOf)
			.collect(Collectors.joining(",")) + "]";

	@Nullable
	private TestCaseIdEntry getTestCaseId(TestCaseId testCaseId, ITestResult testResult) {
		if (testCaseId.parametrized()) {
			return TestCaseIdUtils.getParameterizedTestCaseId(testResult.getMethod().getConstructorOrMethod().getMethod(),
					testResult.getParameters()
			);
		}
		return new TestCaseIdEntry(testCaseId.value());
	}

	protected Set<ItemAttributesRQ> createStepAttributes(ITestResult testResult) {
		Attributes attributesAnnotation = getMethodAnnotation(Attributes.class, testResult);
		if (attributesAnnotation != null) {
			return AttributeParser.retrieveAttributes(attributesAnnotation);
		}
		return null;
	}

	/**
	 * Returns method annotation by specified annotation class from
	 * from TestNG Method or null if the method does not contain
	 * such annotation.
	 *
	 * @param annotation Annotation class to find
	 * @param testResult Where to find
	 * @return {@link Annotation} or null if doesn't exists
	 */
	private <T extends Annotation> T getMethodAnnotation(Class<T> annotation, ITestResult testResult) {
		ITestNGMethod testNGMethod = testResult.getMethod();
		if (null != testNGMethod) {
			ConstructorOrMethod constructorOrMethod = testNGMethod.getConstructorOrMethod();
			if (null != constructorOrMethod) {
				Method method = constructorOrMethod.getMethod();
				if (null != method) {
					return method.getAnnotation(annotation);
				}
			}
		}
		return null;
	}

	/**
	 * Checks if test suite has any methods to run.
	 * It can be useful with writing test with "groups".
	 * So there could be created a test suite that has some methods but doesn't fit
	 * the condition of a group. Such suite should be ignored for rp.
	 *
	 * @param testContext Test context
	 * @return True if item has any tests to run
	 */
	private boolean hasMethodsToRun(ITestContext testContext) {
		return null != testContext && null != testContext.getAllTestMethods() && 0 != testContext.getAllTestMethods().length;
	}

	/**
	 * Calculate parent id for configuration
	 */
	@VisibleForTesting
	Maybe<String> getConfigParent(ITestResult testResult, TestMethodType type) {
		Maybe<String> parentId;
		if (TestMethodType.BEFORE_SUITE.equals(type) || TestMethodType.AFTER_SUITE.equals(type)) {
			parentId = getAttribute(testResult.getTestContext().getSuite(), RP_ID);
		} else {
			parentId = getAttribute(testResult.getTestContext(), RP_ID);
		}
		return parentId;
	}

	private boolean isRetry(ITestResult result) {
		IRetryAnalyzer retryAnalyzer = result.getMethod().getRetryAnalyzer(result);
		return Objects.nonNull(retryAnalyzer) && retryAnalyzer.retry(result);
	}

	@VisibleForTesting
	static class MemorizingSupplier<T> implements Supplier<T>, Serializable {
		final Supplier<T> delegate;
		transient volatile T value;
		private static final long serialVersionUID = 0L;

		MemorizingSupplier(Supplier<T> delegate) {
			this.delegate = delegate;
		}

		public T get() {
			if (value == null) {
				synchronized (this) {
					if (value == null) {
						return (value = delegate.get());
					}
				}
			}
			return value;
		}

		public void reset() {
			value = null;
		}

		public String toString() {
			return "Suppliers.memoize(" + this.delegate + ")";
		}
	}
}
