package com.epam.reportportal.testng;

import static com.epam.reportportal.testng.TestNGService.DESCRIPTION_ERROR_FORMAT;
import static com.epam.reportportal.testng.integration.feature.description.DescriptionFailedTest.ASSERT_ERROR;
import static com.epam.reportportal.testng.integration.feature.description.DescriptionFailedTest.NO_SUCH_ELEMENT_EXCEPTION;
import static com.epam.reportportal.testng.integration.feature.description.DescriptionFailedTest.TEST_DESCRIPTION;
import static com.epam.reportportal.testng.integration.util.TestUtils.mockLaunch;
import static com.epam.reportportal.testng.integration.util.TestUtils.mockLogging;
import static com.epam.reportportal.testng.integration.util.TestUtils.namedUuid;
import static com.epam.reportportal.testng.integration.util.TestUtils.runTests;
import static com.epam.reportportal.testng.integration.util.TestUtils.standardParameters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.integration.feature.description.DescriptionFailedTest;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestDescriptionFailedTest {

  public static class TestListener extends BaseTestNGListener {
    public static final ThreadLocal<ReportPortal> REPORT_PORTAL_THREAD_LOCAL = new ThreadLocal<>();

    public TestListener() {
      super(new TestNGService(new MemoizingSupplier<>(() -> getLaunch(REPORT_PORTAL_THREAD_LOCAL.get().getParameters()))));
    }

    public static void initReportPortal(ReportPortal reportPortal) {
      REPORT_PORTAL_THREAD_LOCAL.set(reportPortal);
    }

    private static Launch getLaunch(ListenerParameters parameters) {

      ReportPortal reportPortal = REPORT_PORTAL_THREAD_LOCAL.get();
      StartLaunchRQ rq = new StartLaunchRQ();
      rq.setName(parameters.getLaunchName());
      rq.setStartTime(Calendar.getInstance().getTime());
      rq.setMode(parameters.getLaunchRunningMode());
      rq.setStartTime(Calendar.getInstance().getTime());

      return reportPortal.newLaunch(rq);

    }
  }

  private final String suitedUuid = namedUuid("suite");
  private final String testClassUuid = namedUuid("class");
  private final String assertionError = "java.lang.AssertionError: " + ASSERT_ERROR;
  private final String noSuchElementException = "java.util.NoSuchElementException: " + NO_SUCH_ELEMENT_EXCEPTION;
  private final String empty = "";
  private final String testDescriptionTestAssertErrorMessage = String.format(DESCRIPTION_ERROR_FORMAT, TEST_DESCRIPTION, assertionError);
  private final String testDescriptionTestExceptionMessage = String.format(DESCRIPTION_ERROR_FORMAT, TEST_DESCRIPTION, noSuchElementException);
  private final String testWithoutDescriptionTestAssertErrorMessage = String.format(DESCRIPTION_ERROR_FORMAT, empty, assertionError).trim();
  private final String testWithoutDescriptionTestExceptionMessage = String.format(DESCRIPTION_ERROR_FORMAT, empty, noSuchElementException).trim();

  @Mock
  private ReportPortalClient client;

  @BeforeEach
  public void initMocks() {
    mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, "test");
    mockLogging(client);
    ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
    TestListener.initReportPortal(reportPortal);
  }

  @Test
  public void verify_description_in_case_of_test_exceptions() {
    runTests(Collections.singletonList(TestDescriptionFailedTest.TestListener.class), DescriptionFailedTest.class);

    verify(client, times(1)).startLaunch(any()); // Start launch
    verify(client, times(1)).startTestItem(any());  // Start parent suites
    verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

    ArgumentCaptor<String> finishUuidCapture = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
    verify(client, times(7)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

    List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
    FinishTestItemRQ testDescriptionTestAssertError = finishItems.get(0);
    FinishTestItemRQ testDescriptionTestException = finishItems.get(1);
    FinishTestItemRQ testWithDescriptionAnnotationTestException = finishItems.get(2);
    FinishTestItemRQ testWithoutDescriptionTestAssertError = finishItems.get(3);
    FinishTestItemRQ testWithoutDescriptionTestException = finishItems.get(4);

    assertThat(testDescriptionTestAssertError.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testDescriptionTestException.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testWithoutDescriptionTestAssertError.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testWithoutDescriptionTestException.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testWithDescriptionAnnotationTestException.getStatus(), equalTo(ItemStatus.FAILED.name()));

    assertThat(testDescriptionTestAssertError.getDescription(), startsWith(testDescriptionTestAssertErrorMessage));
    assertThat(testDescriptionTestException.getDescription(), startsWith(testDescriptionTestExceptionMessage));
    assertThat(testWithoutDescriptionTestAssertError.getDescription(), startsWith(testWithoutDescriptionTestAssertErrorMessage));
    assertThat(testWithoutDescriptionTestException.getDescription(), startsWith(testWithoutDescriptionTestExceptionMessage));
    assertThat(testWithDescriptionAnnotationTestException.getDescription(), startsWith(testDescriptionTestExceptionMessage));
  }
}
