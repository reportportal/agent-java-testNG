package com.epam.reportportal.testng;

import com.epam.reportportal.guice.Injector;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import rp.com.google.inject.Module;

/**
 * In case in your testng.xml, in test tag, you have multiple classes, default
 * ReportPortalTestNGListener will save them all in one group.
 *
 * This class ignores test tag and group tests by class name instead.
 */
public class ReportPortalTestNGListenerGropByClass extends ReportPortalTestNGListener {

  /**
   * Map canonical class name to your test item id.
   *
   * com.company.smoke.OneTest -> 2r2a4wer234
   */
  final protected static Map<String, TestResultsPerClass> results = new HashMap<String, TestResultsPerClass>();

  public ReportPortalTestNGListenerGropByClass() {
    super();
  }

  public ReportPortalTestNGListenerGropByClass(final Module... extensions) {
    super(extensions);
  }

  public ReportPortalTestNGListenerGropByClass(Injector injector) {
    super(injector);
  }

  @Override
  public void startConfiguration(ITestResult testResult) {
    TestResultsPerClass result = createNewItemForClass(testResult);
    StartTestItemRQ rq = buildStartConfigurationRq(testResult);
    final Maybe<String> itemID = reportPortal.startTestItem(result.rp_id, rq);
    testResult.setAttribute(RP_ID, itemID);
  }

  @Override
  public void onTestStart(ITestResult testResult) {
    TestResultsPerClass result = createNewItemForClass(testResult);
    if (testResult.getAttribute(RP_ID) != null) {
      return;
    }
    StartTestItemRQ rq = buildStartStepRq(testResult);
    Maybe<String> stepMaybe = reportPortal.startTestItem(result.rp_id, rq);
    testResult.setAttribute(RP_ID, stepMaybe);
  }

  /**
   * Class level: create new node in RP
   */
  protected TestResultsPerClass createNewItemForClass(ITestResult testResult) {
    Class theClass = testResult.getTestClass().getRealClass();
    String classCanonicalName = theClass.getCanonicalName();
    TestResultsPerClass result;
    synchronized (results) {
      result = results.get(classCanonicalName);
      if (result == null) {
        result = new TestResultsPerClass();
        results.put(classCanonicalName, result);
      }
      if (result.rp_id == null) {
        // This is the first method for the class, lets create testItem in RP.
        Maybe<String> parentId = getRP_ID(testResult.getTestContext().getSuite());
        StartTestItemRQ rq = buildStartTestItemRq(theClass);
        result.rp_id = reportPortal.startTestItem(parentId, rq);
      }
    }
    return result;
  }

  protected StartTestItemRQ buildStartTestItemRq(Class theClass) {
    StartTestItemRQ rq = new StartTestItemRQ();
    rq.setName(theClass.getSimpleName());
    rq.setStartTime(Calendar.getInstance().getTime());
    rq.setType("TEST");
    return rq;
  }

  @Override
  public void finishTestMethod(String status, ITestResult testResult) {
    super.finishTestMethod(status, testResult);

    // Calculate, is the whole class is over, so we may close test group.
    /*
    Class testClass = testResult.getTestClass().getRealClass();
    synchronized (results) {
      final TestResultsPerClass resultsPerClass = results.get(testClass.getCanonicalName());
      resultsPerClass.storeResult(status);
      resultsPerClass.methodsRemain--;
      if (resultsPerClass.methodsRemain <= 0) {
        reportFinishTestClass(new Date(testResult.getEndMillis()), resultsPerClass);
      }
    }
    */
  }

  protected void reportFinishTestClass(Date time, TestResultsPerClass resultsPerClass) {
    FinishTestItemRQ rq = new FinishTestItemRQ();
    rq.setEndTime(time);
    rq.setStatus(resultsPerClass.status);
    reportPortal.finishTestItem(resultsPerClass.rp_id, rq);
  }

  @Override
  public void onStart(ITestContext testContext) {
    // ignore super method, we should skip single <test> from testng.xml
    // Increase counter, to finish testItem when all method complete and we have counter=0
    /*
    synchronized (results) {
      for (ITestNGMethod method : testContext.getAllTestMethods()) {
        final String className = method.getRealClass().getCanonicalName();
        TestResultsPerClass result = results.get(className);
        if (result == null) {
          result = new TestResultsPerClass();
          results.put(className, result);
        }
      }
    }
    */
  }

  @Override
  public void onFinish(ITestContext testContext) {
    // ignore super method as well as onStart(ITestContext testContext)
    // Just as backup, close test classes groups that not closed by some reason.
    synchronized (results) {
      for (String className : results.keySet()) {
        TestResultsPerClass result = results.get(className);
//        if (result.methodsRemain > 0) {
//          LOGGER.warn("By some reason we did not close class" + className);
          reportFinishTestClass(new Date(), result);
//        }
      }
    }
  }
}
