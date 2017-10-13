package com.epam.reportportal.testng;

import com.epam.reportportal.guice.Injector;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.testng.ISuite;
import org.testng.ITestContext;
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
   * com.company.smoke.OneTest -> TestResultsPerClass[rp_id:2r2a4wer234, status: PASSED]
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
    String classCanonicalName = testResult.getTestClass().getRealClass().getCanonicalName();
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
        StartTestItemRQ rq = buildStartTestItemRq(testResult);
        result.rp_id = reportPortal.startTestItem(parentId, rq);
      }
    }
    return result;
  }

  protected StartTestItemRQ buildStartTestItemRq(ITestResult result) {
    StartTestItemRQ rq = new StartTestItemRQ();
    String name = result.getTestName();
    if (name == null || name.trim().length() == 0) {
      name = result.getTestClass().getRealClass().getSimpleName();
    }
    rq.setName(name);
    rq.setDescription(result.getTestClass().getRealClass().getSimpleName());
    rq.setStartTime(Calendar.getInstance().getTime());
    rq.setType("TEST");
    return rq;
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
  }

  @Override
  public void onFinish(ITestContext testContext) {
    // ignore super method as well as onStart(ITestContext testContext)
  }

  @Override
  public void onFinish(ISuite suite) {
    // Close test classes groups that not closed by some reason.
    synchronized (results) {
      for (String className : results.keySet()) {
        TestResultsPerClass result = results.get(className);
        reportFinishTestClass(new Date(), result);
      }
      results.clear();
    }
    super.onFinish(suite);
  }
}
