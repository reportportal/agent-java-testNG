package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.Statuses;
import io.reactivex.Maybe;

public class TestResultsPerClass {
  public Maybe<String> rp_id;
  public String status = Statuses.PASSED;

  /* Merge new test result item with old test results
 Implement the following matrix logic (first column is status, second newStatus):
  + + --> +
  + - --> -
  - + --> -
  - - --> -
  s + --> s
  + s --> s
  - s --> -
  s - --> -
   */
  public void storeResult(String newStatus) {
    if (status == Statuses.FAILED || newStatus == Statuses.PASSED) {
      return;
    }
    if (newStatus == Statuses.FAILED) {
      status = Statuses.FAILED;
      return;
    }
    status = Statuses.SKIPPED;
  }
}
