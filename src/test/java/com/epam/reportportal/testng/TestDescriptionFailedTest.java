package com.epam.reportportal.testng;

import static com.epam.reportportal.testng.integration.util.TestUtils.mockLaunch;
import static com.epam.reportportal.testng.integration.util.TestUtils.mockLogging;
import static com.epam.reportportal.testng.integration.util.TestUtils.namedUuid;
import static com.epam.reportportal.testng.integration.util.TestUtils.runTests;
import static com.epam.reportportal.testng.integration.util.TestUtils.standardParameters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.bug.TestSkipExceptionOnBeforeClassTest;
import com.epam.reportportal.testng.integration.feature.description.DescriptionFailedTest;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class TestDescriptionFailedTest {

  private final String suitedUuid = namedUuid("suite");
  private final String testClassUuid = namedUuid("class");
  private final String assertionError = "AssertionError";
  private final String noSuchElementException = "NoSuchElementException";
  private final String empty = "";
  private final String testDescriptionTestAssertErrorMessage = String.format(DESCRIPTION_ERROR_FORMAT, TEST_DESCRIPTION, assertionError, ASSERT_ERROR);
  private final String testDescriptionTestExceptionMessage = String.format(DESCRIPTION_ERROR_FORMAT, TEST_DESCRIPTION, noSuchElementException, NO_SUCH_ELEMENT_EXCEPTION);
  private final String testWithoutDescriptionTestAssertErrorMessage = String.format(DESCRIPTION_ERROR_FORMAT, empty, assertionError, ASSERT_ERROR).trim();
  private final String testWithoutDescriptionTestExceptionMessage = String.format(DESCRIPTION_ERROR_FORMAT, empty, noSuchElementException, NO_SUCH_ELEMENT_EXCEPTION).trim();
  private final String testSuiteMessage = String.format(DESCRIPTION_ERROR_FORMAT, empty, noSuchElementException, NO_SUCH_ELEMENT_EXCEPTION).trim();

  @Mock
  private ReportPortalClient client;

  @BeforeEach
  public void initMocks() {
    mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, "test");
    mockLogging(client);
    ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
    TestSkipExceptionOnBeforeClassTest.TestListener.initReportPortal(reportPortal);
  }

  @Test
  public void verify_description_in_case_of_test_exceptions() {
    runTests(Collections.singletonList(TestSkipExceptionOnBeforeClassTest.TestListener.class), DescriptionFailedTest.class);

    verify(client, times(1)).startLaunch(any()); // Start launch
    verify(client, times(1)).startTestItem(any());  // Start parent suites
    verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

    ArgumentCaptor<String> finishUuidCapture = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
    verify(client, times(6)).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());

    List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();
    FinishTestItemRQ testDescriptionTestAssertError = finishItems.get(0);
    FinishTestItemRQ testDescriptionTestException = finishItems.get(1);
    FinishTestItemRQ testWithoutDescriptionTestAssertError = finishItems.get(2);
    FinishTestItemRQ testWithoutDescriptionTestException = finishItems.get(3);
    FinishTestItemRQ testSuite = finishItems.get(4);

    assertThat(testDescriptionTestAssertError.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testDescriptionTestException.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testWithoutDescriptionTestAssertError.getStatus(), equalTo(ItemStatus.FAILED.name()));
    assertThat(testWithoutDescriptionTestException.getStatus(), equalTo(ItemStatus.FAILED.name()));

    assertThat(testDescriptionTestAssertError.getDescription(), equalTo(testDescriptionTestAssertErrorMessage));
    assertThat(testDescriptionTestException.getDescription(), equalTo(testDescriptionTestExceptionMessage));
    assertThat(testWithoutDescriptionTestAssertError.getDescription(), equalTo(testWithoutDescriptionTestAssertErrorMessage));
    assertThat(testWithoutDescriptionTestException.getDescription(), equalTo(testWithoutDescriptionTestExceptionMessage));
    assertThat(testSuite.getDescription(), equalTo(testSuiteMessage));
  }
}
