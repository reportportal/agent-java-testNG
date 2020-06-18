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
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.LaunchImpl;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.analytics.GoogleAnalytics;
import com.epam.reportportal.service.analytics.item.AnalyticsEvent;
import com.epam.reportportal.service.analytics.item.AnalyticsItem;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.testng.util.internal.LimitedSizeConcurrentHashMap;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.reportportal.utils.properties.ClientProperties;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.annotations.Nullable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;
import rp.com.google.common.annotations.VisibleForTesting;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.testng.util.ItemTreeUtils.createKey;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * TestNG service implements operations for interaction report portal
 */
public class TestNGService implements ITestNGService {

	private static final String AGENT_PROPERTIES_FILE = "agent.properties";
	private static final String CLIENT_PROPERTIES_FILE = "client.properties";
	private static final String START_LAUNCH_EVENT_ACTION = "Start launch";
	private static final Predicate<StackTraceElement> IS_RETRY_ELEMENT = e -> "org.testng.internal.TestInvoker".equals(e.getClassName())
			&& "retryFailed".equals(e.getMethodName());
	private static final Predicate<StackTraceElement[]> IS_RETRY = eList -> Arrays.stream(eList).anyMatch(IS_RETRY_ELEMENT);
	private static final int MAXIMUM_HISTORY_SIZE = 1000;

	public static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	public static final String RP_ID = "rp_id";
	public static final String RP_RETRY = "rp_retry";
	public static final String RP_METHOD_TYPE = "rp_method_type";
	public static final String ARGUMENT = "arg";
	public static final String NULL_VALUE = "NULL";
	public static final TestItemTree ITEM_TREE = new TestItemTree();
	public static final Issue NOT_ISSUE;

	static {
		NOT_ISSUE = new Issue();
		NOT_ISSUE.setIssueType(LaunchImpl.NOT_ISSUE);
	}

	private static ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	private final AtomicBoolean isLaunchFailed = new AtomicBoolean();

	private final Map<Object, Queue<Pair<Maybe<String>, FinishTestItemRQ>>> BEFORE_METHOD_TRACKER = new ConcurrentHashMap<>();

	private final Map<Object, Boolean> RETRY_STATUS_TRACKER = new LimitedSizeConcurrentHashMap<>(MAXIMUM_HISTORY_SIZE);
	private final Map<Object, Boolean> SKIPPED_STATUS_TRACKER = new LimitedSizeConcurrentHashMap<>(MAXIMUM_HISTORY_SIZE);

	private final MemorizingSupplier<Launch> launch;

	private final ExecutorService googleAnalyticsExecutor = Executors.newSingleThreadExecutor();
	private final GoogleAnalytics googleAnalytics = new GoogleAnalytics(Schedulers.from(googleAnalyticsExecutor), "UA-96321031-1");
	private final List<AnalyticsItem> analyticsItems = new CopyOnWriteArrayList<>();
	private final List<Completable> dependencies = new CopyOnWriteArrayList<>();

	private static Thread getShutdownHook(final Launch launch) {
		return new Thread(() -> {
			FinishExecutionRQ rq = new FinishExecutionRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			launch.finish(rq);
		});
	}

	public TestNGService() {
		this.launch = new MemorizingSupplier<>(() -> {
			//this reads property, so we want to
			//init ReportPortal object each time Launch object is going to be created

			StartLaunchRQ rq = buildStartLaunchRq(REPORT_PORTAL.getParameters());
			rq.setStartTime(Calendar.getInstance().getTime());
			addStartLaunchEvent(rq);
			Launch newLaunch = REPORT_PORTAL.newLaunch(rq);
			Runtime.getRuntime().addShutdownHook(getShutdownHook(newLaunch));
			return newLaunch;
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

	protected GoogleAnalytics getGoogleAnalytics() {
		return googleAnalytics;
	}

	@Override
	public void startLaunch() {
		Maybe<String> launchId = this.launch.get().start();
		StepAspect.addLaunch("default", this.launch.get());
		ITEM_TREE.setLaunchId(launchId);
		dependencies.addAll(analyticsItems.stream()
				.map(it -> launchId.flatMap(l -> getGoogleAnalytics().send(it)))
				.map(Maybe::ignoreElement)
				.collect(toList()));
	}

	@Override
	public void finishLaunch() {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(isLaunchFailed.get() ? ItemStatus.FAILED.name() : ItemStatus.PASSED.name());
		launch.get().finish(rq);
		try {
			Completable.concat(dependencies).timeout(launch.get().getParameters().getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
		} finally {
			googleAnalytics.close();
			googleAnalyticsExecutor.shutdown();
			try {
				this.googleAnalyticsExecutor.awaitTermination(launch.get().getParameters().getReportingTimeout(), TimeUnit.SECONDS);
			} catch (InterruptedException exc) {
				//do nothing
			} finally {
				this.launch.reset();
			}
		}
	}

	private void addToTree(ISuite suite, Maybe<String> item) {
		ITEM_TREE.getTestItems().put(createKey(suite), TestItemTree.createTestItemLeaf(item, suite.getXmlSuite().getTests().size()));
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

	@SuppressWarnings("unchecked")
	protected <T> T getAttribute(IAttributes attributes, String attribute) {
		return (T) attributes.getAttribute(attribute);
	}

	@Override
	public void finishTestSuite(ISuite suite) {
		Maybe<String> rpId = getAttribute(suite, RP_ID);
		if (null != rpId) {
			FinishTestItemRQ rq = buildFinishTestSuiteRq(suite);
			launch.get().finishTestItem(rpId, rq);
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

	private static Set<ITestResult> getTestResults(IResultMap rm) {
		return ofNullable(rm).map(IResultMap::getAllResults).orElse(Collections.emptySet());
	}

	@Override
	public void finishTest(ITestContext testContext) {
		if (hasMethodsToRun(testContext)) {
			FinishTestItemRQ rq = buildFinishTestRq(testContext);
			launch.get().finishTestItem(this.getAttribute(testContext, RP_ID), rq);
			if (launch.get().getParameters().isCallbackReportingEnabled()) {
				removeFromTree(testContext);
			}
			// Cleanup
			Set<ITestResult> results = new HashSet<>();
			results.addAll(getTestResults(testContext.getFailedButWithinSuccessPercentageTests()));
			results.addAll(getTestResults(testContext.getFailedConfigurations()));
			results.addAll(getTestResults(testContext.getFailedTests()));
			results.addAll(getTestResults(testContext.getSkippedTests()));
			results.addAll(getTestResults(testContext.getSkippedConfigurations()));
			results.addAll(getTestResults(testContext.getPassedConfigurations()));
			results.addAll(getTestResults(testContext.getPassedTests()));
			results.stream().map(ITestResult::getInstance).filter(Objects::nonNull).collect(Collectors.toSet()).forEach(i -> {
				RETRY_STATUS_TRACKER.remove(i);
				SKIPPED_STATUS_TRACKER.remove(i);
			});
		}
	}

	private void removeFromTree(ITestContext testContext) {
		ofNullable(ITEM_TREE.getTestItems().get(createKey(testContext.getSuite()))).ifPresent(suiteLeaf -> suiteLeaf.getChildItems()
				.remove(createKey(testContext)));
	}

	private boolean isRetry(ITestResult testResult) {
		if (testResult.wasRetried()) {
			return true;
		}
		Object instance = testResult.getInstance();
		if (instance != null && RETRY_STATUS_TRACKER.containsKey(instance)) {
			return true;
		}
		return IS_RETRY.test(Thread.currentThread().getStackTrace());
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
		rq.setName(testResult.getMethod().getMethodName());
		rq.setCodeRef(testResult.getMethod().getQualifiedName());
		rq.setDescription(testResult.getMethod().getDescription());
		rq.setStartTime(new Date(testResult.getStartMillis()));
		rq.setType(type == null ? null : type.toString());
		boolean retry = isRetry(testResult);
		if (retry) {
			rq.setRetry(Boolean.TRUE);
		}
		return rq;
	}

	@Override
	public void startConfiguration(ITestResult testResult) {
		TestMethodType type = TestMethodType.getStepType(testResult.getMethod());
		testResult.setAttribute(RP_METHOD_TYPE, type);
		StartTestItemRQ rq = buildStartConfigurationRq(testResult, type);
		if (Boolean.TRUE == rq.isRetry()) {
			testResult.setAttribute(RP_RETRY, Boolean.TRUE);
		}
		Maybe<String> parentId = getConfigParent(testResult, type);
		Maybe<String> itemID = launch.get().startTestItem(parentId, rq);
		testResult.setAttribute(RP_ID, itemID);
		StepAspect.setParentId(itemID);
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param testResult TestNG's testResult context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(final @NotNull ITestResult testResult) {
		return buildStartStepRq(testResult, ofNullable(TestMethodType.getStepType(testResult.getMethod())).orElse(TestMethodType.STEP));
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param testResult TestNG's testResult context
	 * @param type       method type
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(final @NotNull ITestResult testResult, final @NotNull TestMethodType type) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(createStepName(testResult));
		String codeRef = testResult.getMethod().getQualifiedName();
		rq.setCodeRef(codeRef);
		rq.setTestCaseId(Objects.requireNonNull(getTestCaseId(codeRef, testResult)).getId());
		rq.setAttributes(createStepAttributes(testResult));
		rq.setDescription(createStepDescription(testResult));
		rq.setParameters(createStepParameters(testResult));
		rq.setUniqueId(extractUniqueID(testResult));
		rq.setStartTime(new Date(testResult.getStartMillis()));
		rq.setType(type.toString());
		boolean retry = isRetry(testResult);
		if (retry) {
			rq.setRetry(Boolean.TRUE);
		}
		return rq;
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
	public void startTestMethod(ITestResult testResult) {
		TestMethodType methodType = ofNullable(TestMethodType.getStepType(testResult.getMethod())).orElse(TestMethodType.STEP);
		testResult.setAttribute(RP_METHOD_TYPE, methodType);
		StartTestItemRQ rq = buildStartStepRq(testResult, methodType);
		if (Boolean.TRUE == rq.isRetry()) {
			testResult.setAttribute(RP_RETRY, Boolean.TRUE);
		}

		Maybe<String> stepMaybe = launch.get().startTestItem(getAttribute(testResult.getTestContext(), RP_ID), rq);
		testResult.setAttribute(RP_ID, stepMaybe);
		StepAspect.setParentId(stepMaybe);
		if (launch.get().getParameters().isCallbackReportingEnabled()) {
			addToTree(testResult, stepMaybe);
		}
	}

	/**
	 * Extension point to customize test method on it's finish
	 *
	 * @param status     item execution status
	 * @param testResult TestNG's testResult context
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishTestMethodRq(ItemStatus status, ITestResult testResult) {
		return buildFinishTestMethodRq(status.name(), testResult);
	}

	/**
	 * @param status     item execution status
	 * @param testResult TestNG's testResult context
	 * @return Request to ReportPortal
	 * @deprecated use {@link #buildFinishTestMethodRq(ItemStatus, ITestResult)}
	 */
	@Deprecated
	protected FinishTestItemRQ buildFinishTestMethodRq(String status, ITestResult testResult) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(new Date(testResult.getEndMillis()));
		rq.setStatus(status);
		return rq;
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

	private void processFinishRetryFlag(ITestResult testResult, FinishTestItemRQ rq) {
		Object instance = testResult.getInstance();
		if (instance != null && !ItemStatus.SKIPPED.name().equals(rq.getStatus())) {
			// Remove retry flag if an item passed
			RETRY_STATUS_TRACKER.remove(instance);
		}

		TestMethodType type = getAttribute(testResult, RP_METHOD_TYPE);

		boolean isRetried = testResult.wasRetried();
		if (TestMethodType.STEP == type && getAttribute(testResult, RP_RETRY) == null && isRetried) {
			RETRY_STATUS_TRACKER.put(instance, Boolean.TRUE);
			rq.setRetry(Boolean.TRUE);
			rq.setIssue(NOT_ISSUE);
		}
		if (isRetried) {
			testResult.setAttribute(RP_RETRY, Boolean.TRUE);
		}

		// Save before method finish requests to update them with a retry flag in case of main test method failed
		if (instance != null) {
			if (TestMethodType.BEFORE_METHOD == type && getAttribute(testResult, RP_RETRY) == null) {
				Maybe<String> itemId = getAttribute(testResult, RP_ID);
				BEFORE_METHOD_TRACKER.computeIfAbsent(instance, i -> new ConcurrentLinkedQueue<>()).add(Pair.of(itemId, rq));
			} else {
				Queue<Pair<Maybe<String>, FinishTestItemRQ>> beforeFinish = BEFORE_METHOD_TRACKER.remove(instance);
				if (beforeFinish != null && isRetried) {
					beforeFinish.stream().filter(e -> e.getValue().isRetry() == null || !e.getValue().isRetry()).forEach(e -> {
						FinishTestItemRQ f = e.getValue();
						f.setRetry(true);
						launch.get().finishTestItem(e.getKey(), f);
					});
				}
			}
		}
	}

	/**
	 * Extension point to customize skipped test insides
	 *
	 * @param testResult TestNG's testResult context
	 */
	protected void createSkippedSteps(ITestResult testResult) {
	}

	@Override
	public void finishTestMethod(String statusStr, ITestResult testResult) {
		ItemStatus status = ItemStatus.valueOf(statusStr);
		Maybe<String> itemId = getAttribute(testResult, RP_ID);

		if (ItemStatus.SKIPPED == status) {
			if (!testResult.wasRetried() && null == itemId) {
				startTestMethod(testResult);
				itemId = getAttribute(testResult, RP_ID); // if we started new test method we need to get new item ID
			}
			createSkippedSteps(testResult);
		}

		launch.get().getStepReporter().finishPreviousStep();
		FinishTestItemRQ rq = buildFinishTestMethodRq(status, testResult);

		TestMethodType type = getAttribute(testResult, RP_METHOD_TYPE);
		Object instance = testResult.getInstance();

		// TestNG does not repeat before methods if an after method fails during retries. But reports them as skipped.
		// Mark before methods as not an issue if it is not a culprit.
		if (instance != null) {
			if (ItemStatus.FAILED == status && TestMethodType.BEFORE_METHOD == type) {
				SKIPPED_STATUS_TRACKER.put(instance, Boolean.TRUE);
			}
			if (ItemStatus.SKIPPED == status && (SKIPPED_STATUS_TRACKER.containsKey(instance) || (TestMethodType.BEFORE_METHOD == type
					&& getAttribute(testResult, RP_RETRY) != null))) {
				rq.setIssue(NOT_ISSUE);
			}
		}

		processFinishRetryFlag(testResult, rq);

		Maybe<OperationCompletionRS> finishItemResponse = launch.get().finishTestItem(itemId, rq);
		if (launch.get().getParameters().isCallbackReportingEnabled()) {
			updateTestItemTree(finishItemResponse, testResult);
		}
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
		String status = isTestPassed(testContext) ? ItemStatus.PASSED.name() : ItemStatus.FAILED.name();
		rq.setStatus(status);
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
		parameters.addAll(createDataProviderParameters(testResult));
		parameters.addAll(createAnnotationParameters(testResult));
		parameters.addAll(crateFactoryParameters(testResult));
		return parameters.isEmpty() ? null : parameters;
	}

	private void addStartLaunchEvent(StartLaunchRQ rq) {
		AnalyticsEvent.AnalyticsEventBuilder analyticsEventBuilder = AnalyticsEvent.builder();
		analyticsEventBuilder.withAction(START_LAUNCH_EVENT_ACTION);
		SystemAttributesExtractor.extract(CLIENT_PROPERTIES_FILE, TestNGService.class.getClassLoader(), ClientProperties.CLIENT)
				.stream()
				.findFirst()
				.ifPresent(clientAttribute -> analyticsEventBuilder.withCategory(clientAttribute.getValue()));

		rq.getAttributes()
				.stream()
				.filter(attribute -> attribute.isSystem() && DefaultProperties.AGENT.getName().equalsIgnoreCase(attribute.getKey()))
				.findFirst()
				.ifPresent(agentAttribute -> analyticsEventBuilder.withLabel(agentAttribute.getValue()));
		analyticsItems.add(analyticsEventBuilder.build());
	}

	/**
	 * Process testResult to create parameters provided via {@link Parameters}
	 *
	 * @param testResult TestNG's testResult context
	 * @return Step Parameters being sent to Report Portal
	 */
	private List<ParameterResource> createAnnotationParameters(ITestResult testResult) {
		Parameters parametersAnnotation = getMethodAnnotation(Parameters.class, testResult);
		if (parametersAnnotation == null) {
			return Collections.emptyList();
		}
		String[] keys = parametersAnnotation.value();
		Object[] parameters = testResult.getParameters();
		if (parameters.length != keys.length || keys.length <= 0) {
			return Collections.emptyList();
		}
		return IntStream.range(0, keys.length).mapToObj(i -> {
			ParameterResource parameter = new ParameterResource();
			parameter.setKey(keys[i]);
			parameter.setValue(parameters[i] == null ? NULL_VALUE : parameters[i].toString());
			return parameter;
		}).collect(toList());
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
		Test testAnnotation = getMethodAnnotation(Test.class, testResult);
		if (null == testAnnotation || isNullOrEmpty(testAnnotation.dataProvider())) {
			return Collections.emptyList();
		}
		List<ParameterResource> result = Lists.newArrayList();
		Annotation[][] parameterAnnotations = testResult.getMethod().getConstructorOrMethod().getMethod().getParameterAnnotations();
		Object[] values = testResult.getParameters();
		int length = parameterAnnotations.length;
		if (length != values.length) {
			return result;
		}

		return IntStream.range(0, length).mapToObj(i -> {
			Object p = values[i];
			ParameterResource parameter = new ParameterResource();
			if (p == null) {
				parameter.setKey(ARGUMENT + i);
				parameter.setValue(NULL_VALUE);
			} else {
				String key = Arrays.stream(parameterAnnotations[i])
						.filter(a -> ParameterKey.class.equals(a.annotationType()))
						.map(a -> ((ParameterKey) a).value())
						.findFirst()
						.orElseGet(() -> p.getClass().getCanonicalName());
				parameter.setKey(key);
				parameter.setValue(p.toString());
			}
			return parameter;
		}).collect(toList());
	}

	private List<ParameterResource> crateFactoryParameters(ITestResult testResult) {
		Object[] parameters = testResult.getFactoryParameters();
		if (parameters == null || parameters.length <= 0) {
			return Collections.emptyList();
		}

		return IntStream.range(0, parameters.length).mapToObj(i -> {
			Object p = parameters[i];
			ParameterResource parameter = new ParameterResource();
			if (p == null) {
				parameter.setKey(ARGUMENT + i);
				parameter.setValue(NULL_VALUE);
			} else {
				parameter.setKey(p.getClass().getCanonicalName());
				parameter.setValue(p.toString());
			}
			return parameter;
		}).collect(toList());
	}

	/**
	 * Extension point to customize test step name
	 *
	 * @param testResult TestNG's testResult context
	 * @return Test/Step Name being sent to ReportPortal
	 */
	protected String createStepName(ITestResult testResult) {
		return testResult.getMethod().getMethodName();
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param testResult TestNG's testResult context
	 * @return Test/Step Description being sent to ReportPortal
	 */
	protected String createStepDescription(ITestResult testResult) {
		return testResult.getMethod().getDescription();
	}

	/**
	 * Extension point to customize test suite status being sent to ReportPortal
	 *
	 * @param suite TestNG's suite
	 * @return Status PASSED/FAILED/etc
	 */
	protected String getSuiteStatus(ISuite suite) {
		Collection<ISuiteResult> suiteResults = suite.getResults().values();
		ItemStatus suiteStatus = ItemStatus.PASSED;
		for (ISuiteResult suiteResult : suiteResults) {
			if (!(isTestPassed(suiteResult.getTestContext()))) {
				suiteStatus = ItemStatus.FAILED;
				break;
			}
		}
		// if at least one suite failed launch should be failed
		isLaunchFailed.compareAndSet(false, suiteStatus == ItemStatus.FAILED);
		return suiteStatus.name();
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
				&& testContext.getSkippedConfigurations().size() == 0 && (testContext.getSkippedTests().size() == 0
				|| testContext.getSkippedTests()
				.getAllResults()
				.stream()
				.allMatch(e -> (boolean) ofNullable(getAttribute(e, RP_RETRY)).orElse(Boolean.FALSE)));
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
	 * TestNG Method or null if the method does not contain
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
