package com.epam.reportportal.testng;

import com.epam.reportportal.listeners.Statuses;
import io.reactivex.Maybe;

public class TestResultsPerClass {
// does not work with dataProviders
  //  public int methodsRemain = 0;
  public Maybe<String> rp_id;
  public String status = Statuses.PASSED;

  public void storeResult(String newStatus) {
    /*
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
