/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-testNG
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.testng;

import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import org.testng.IExecutionListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.testng.internal.ConstructorOrMethod;
import org.testng.internal.IResultListener2;
import rp.com.google.common.base.Function;
import rp.com.google.common.base.Supplier;
import rp.com.google.inject.Module;

/**
 * Report portal custom event listener. Support parallel execution of test methods, suites, test
 * classes.
 */
public class ReportPortalTestNGListener implements IExecutionListener, ISuiteListener, IResultListener2 {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalTestNGListener.class);

  private static final AtomicInteger INSTANCES = new AtomicInteger(0);
  private AtomicBoolean isLaunchFailed = new AtomicBoolean();

  public static final String NOT_ISSUE = "NOT_ISSUE";
  public static final String RP_ID = "rp_id";
  public static final String ARGUMENT = "arg";

  @Inject
  protected ListenerParameters parameters;

  @Inject
  protected ReportPortalClient reportPortalClient;

  protected final Supplier<StartLaunchRQ> launchSupplier;

  protected ReportPortal reportPortal;

  public ReportPortalTestNGListener() {
    this(new Module[]{});
  }

  public ReportPortalTestNGListener(final Module... extensions) {
    this(Injector.createDefault(extensions));
  }

  public ReportPortalTestNGListener(Injector injector) {
    if (INSTANCES.incrementAndGet() > 1) {
      final String error = "WARNING! More than one ReportPortal listener is added";
      LOGGER.warn(error);

      //even if logger is not configured, print the message to default stdout
      System.out.println(error);
      throw new RuntimeException(error);
    }
    injector.injectMembers(this);
    this.launchSupplier = new Supplier<StartLaunchRQ>() {
      @Override
      public StartLaunchRQ get() {
        return buildStartLaunchRq(parameters);
      }
    };

  }

  @Override
  public void onExecutionStart() {
    StartLaunchRQ rq = launchSupplier.get();
    rq.setStartTime(Calendar.getInstance().getTime());
    this.reportPortal = ReportPortal.startLaunch(reportPortalClient, parameters, rq);
  }

  @Override
  public void onExecutionFinish() {
    FinishExecutionRQ rq = new FinishExecutionRQ();
    rq.setEndTime(Calendar.getInstance().getTime());
    rq.setStatus(isLaunchFailed.get() ? Statuses.FAILED : Statuses.PASSED);
    reportPortal.finishLaunch(rq);
  }

  @Override
  public void onStart(ISuite suite) {
       /*
       * sometimes testng triggers on suite start several times.
       * This is why method is synchronized and check for attribute presence is added
       * avoid starting same suite twice
       */
    if (getRP_ID(suite) == null) {
      StartTestItemRQ rq = buildStartSuiteRq(suite);
      final Maybe<String> item = reportPortal.startTestItem(rq);
      suite.setAttribute(RP_ID, item);
    }
  }

  @Override
  public void onFinish(ISuite suite) {
    if (null != suite.getAttribute(RP_ID)) {
        /* 'real' end time */
      Date now = Calendar.getInstance().getTime();
      FinishTestItemRQ rq = new FinishTestItemRQ();
      rq.setEndTime(now);
      rq.setStatus(getSuiteStatus(suite));
      reportPortal.finishTestItem(getRP_ID(suite), rq);
      suite.removeAttribute(RP_ID);
    }
  }

  @Override
  public void onStart(ITestContext testContext) {
    StartTestItemRQ rq = buildStartTestItemRq(testContext);
    final Maybe<String> testID = reportPortal.startTestItem(getRP_ID(testContext.getSuite()), rq);

    testContext.setAttribute(RP_ID, testID);
  }

  @Override
  public void onFinish(ITestContext testContext) {
    FinishTestItemRQ rq = new FinishTestItemRQ();
    rq.setEndTime(testContext.getEndDate());
    String status = isTestPassed(testContext) ? Statuses.PASSED : Statuses.FAILED;

    rq.setStatus(status);
    reportPortal.finishTestItem(getRP_ID(testContext), rq);
  }

  @Override
  public void onTestStart(ITestResult testResult) {
    if (testResult.getAttribute(RP_ID) != null) {
      return;
    }
    StartTestItemRQ rq = buildStartStepRq(testResult);
    Maybe<String> stepMaybe = reportPortal.startTestItem(getRP_ID(testResult.getTestContext()), rq);
    testResult.setAttribute(RP_ID, stepMaybe);
  }

  @Override
  public void onTestSuccess(ITestResult testResult) {
    finishTestMethod(Statuses.PASSED, testResult);
  }

  @Override
  public void onTestFailure(ITestResult testResult) {
    sendReportPortalMsg(testResult);
    finishTestMethod(Statuses.FAILED, testResult);
  }

  @Override
  public void onTestSkipped(ITestResult testResult) {
    onTestStart(testResult);
    finishTestMethod(Statuses.SKIPPED, testResult);
  }

  public void finishTestMethod(String status, ITestResult testResult) {
    reportPortal.finishTestItem(getRP_ID(testResult), getFinishTestItemRQ(status));
  }

  protected FinishTestItemRQ getFinishTestItemRQ(String status) {
    final Date now = Calendar.getInstance().getTime();
    FinishTestItemRQ rq = new FinishTestItemRQ();
    rq.setEndTime(now);
    rq.setStatus(status);
    // Allows indicate that SKIPPED is not to investigate items for WS
    if (status.equals(Statuses.SKIPPED) && !parameters.getSkippedAnIssue()) {
      Issue issue = new Issue();
      issue.setIssueType(NOT_ISSUE);
      rq.setIssue(issue);
    }
    return rq;
  }


  @Override
  public void beforeConfiguration(ITestResult testResult) {
    startConfiguration(testResult);
  }

  @Override
  public void onConfigurationFailure(ITestResult testResult) {
    sendReportPortalMsg(testResult);
    finishTestMethod(Statuses.FAILED, testResult);
  }

  @Override
  public void onConfigurationSuccess(ITestResult testResult) {
    finishTestMethod(Statuses.PASSED, testResult);
  }

  @Override
  public void onConfigurationSkip(ITestResult testResult) {
    startConfiguration(testResult);
    finishTestMethod(Statuses.SKIPPED, testResult);
  }

  // this action temporary doesn't supported by report portal
  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    finishTestMethod(Statuses.FAILED, result);
  }

  public void startConfiguration(ITestResult testResult) {
    TestMethodType type = TestMethodType.getStepType(testResult.getMethod());
    StartTestItemRQ rq = buildStartConfigurationRq(testResult);

    Maybe<String> parentId = getConfigParent(testResult, type);
    final Maybe<String> itemID = reportPortal.startTestItem(parentId, rq);
    testResult.setAttribute(RP_ID, itemID);
  }

  public void sendReportPortalMsg(final ITestResult result) {
    ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
      @Override
      public SaveLogRQ apply(String itemId) {
        SaveLogRQ rq = new SaveLogRQ();
        rq.setTestItemId(itemId);
        rq.setLevel("ERROR");
        rq.setLogTime(Calendar.getInstance().getTime());
        if (result.getThrowable() != null) {
          rq.setMessage(getStackTraceAsString(result.getThrowable()));
        } else {
          rq.setMessage("Test has failed without exception");
        }
        rq.setLogTime(Calendar.getInstance().getTime());

        return rq;
      }
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
    rq.setName(testContext.getName());
    rq.setStartTime(Calendar.getInstance().getTime());
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
    rq.setTags(parameters.getTags());
    rq.setMode(parameters.getLaunchRunningMode());
    if (!isNullOrEmpty(parameters.getDescription())) {
      rq.setDescription(parameters.getDescription());
    }
    return rq;
  }

  /**
   * Extension point to customize beforeXXX creation event/request
   *
   * @param testResult TestNG's testResult context
   * @return Request to ReportPortal
   */
  protected StartTestItemRQ buildStartConfigurationRq(ITestResult testResult) {
    StartTestItemRQ rq = new StartTestItemRQ();
    String configName = testResult.getMethod().getMethodName();
    rq.setName(configName);

    rq.setDescription(testResult.getMethod().getDescription());
    rq.setStartTime(Calendar.getInstance().getTime());
    TestMethodType type = TestMethodType.getStepType(testResult.getMethod());
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
    StartTestItemRQ rq = new StartTestItemRQ();
    String testStepName = testResult.getMethod().getMethodName();
    rq.setName(testStepName);

    rq.setDescription(createStepDescription(testResult));
    List<ParameterResource> params = createStepParameters(testResult);
    rq.setParameters(params);
    // Currently parameters are not displayed on UI, put them to name
    if (params != null && params.size() > 0) {
      StringBuilder sb = new StringBuilder(rq.getName());
      sb.append('(');
      boolean isFirst = true;
      for (ParameterResource param: params) {
        if (!isFirst) {
          sb.append(", ");
        }
        isFirst = false;
        sb.append(param.getKey()).append('=').append(param.getValue());
      }
      sb.append(')');
      // in case we use more than 255 characters, we need to trim the string and put the rest to description
      if (sb.length() > 255) {
        String extra = sb.substring(250);
        rq.setDescription(rq.getDescription() + " The rest parameters are: ..."+extra);
        sb.setLength(250);
        sb.append("...");
      }
      rq.setName(sb.toString());
    }
    rq.setUniqueId(extractUniqueID(testResult));
    rq.setStartTime(Calendar.getInstance().getTime());
    rq.setType(TestMethodType.getStepType(testResult.getMethod()).toString());
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
   * @param testResult TestNG's testResult context
   * @param parametersAnnotation Annotation with parameters
   * @return Step Parameters being sent to Report Portal
   */
  private List<ParameterResource> createAnnotationParameters(ITestResult testResult,
      Parameters parametersAnnotation) {
    List<ParameterResource> params = Lists.newArrayList();
    String[] keys = parametersAnnotation.value();
    Object[] parameters = testResult.getParameters();
    if (parameters.length != keys.length) {
      return params;
    }
    for (int i = 0; i < keys.length; i++) {
      ParameterResource parameter = new ParameterResource();
      parameter.setKey(keys[i]);
      parameter.setValue(parameters[i] != null ? parameters[i].toString() : null);
      params.add(parameter);
    }
    return params;
  }

  /**
   * Processes testResult to create parameters provided by {@link org.testng.annotations.DataProvider}
   * If parameter key isn't provided by {@link ParameterKey} annotation then it will be
   * 'arg[index]'
   *
   * @param testResult TestNG's testResult context
   * @return Step Parameters being sent to ReportPortal
   */

  private List<ParameterResource> createDataProviderParameters(ITestResult testResult) {
    List<ParameterResource> result = Lists.newArrayList();
    Annotation[][] parameterAnnotations = testResult.getMethod().getConstructorOrMethod()
        .getMethod().getParameterAnnotations();
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
      parameter.setValue(value);
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
   * Check is current method passed according the number of failed tests and configurations
   *
   * @param testContext TestNG's test content
   * @return TRUE if passed, FALSE otherwise
   */
  protected boolean isTestPassed(ITestContext testContext) {
    return testContext.getFailedTests().size() == 0
        && testContext.getFailedConfigurations().size() == 0
        && testContext.getSkippedConfigurations().size() == 0
        && testContext.getSkippedTests().size() == 0;
  }

  @SuppressWarnings("unchecked")
  protected Maybe<String> getRP_ID(IAttributes attributes) {
    return (Maybe<String>) attributes.getAttribute(RP_ID);
  }

  /**
   * Returns test item ID from annotation if it provided.
   *
   * @param testResult Where to find
   * @return test item ID or null
   */
  @Nullable
  private String extractUniqueID(ITestResult testResult) {
    UniqueID itemUniqueID = getMethodAnnotation(UniqueID.class, testResult);
    return itemUniqueID != null ? itemUniqueID.value() : null;
  }

  /**
   * Returns method annotation by specified annotation class from from TestNG Method or null if the
   * method does not contain such annotation.
   *
   * @param annotation Annotation class to find
   * @param testResult Where to find
   * @return {@link Annotation} or null if doesn't exsists
   */
  @Nullable
  private <T extends Annotation> T getMethodAnnotation(Class<T> annotation,
      ITestResult testResult) {
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
   * Calculate parent id for configuration
   */
  private Maybe<String> getConfigParent(ITestResult testResult, TestMethodType type) {
    Maybe<String> parentId;
    if (TestMethodType.BEFORE_SUITE.equals(type) || TestMethodType.AFTER_SUITE.equals(type)) {
      parentId = getRP_ID(testResult.getTestContext().getSuite());
    } else {
      parentId = getRP_ID(testResult.getTestContext());
    }
    return parentId;
  }

}
