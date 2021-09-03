package com.epam.reportportal.testng.integration.feature.callback;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.tree.ItemTreeReporter;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.testng.TestNGService;
import com.epam.reportportal.testng.util.ItemTreeUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.Calendar;

import static com.epam.reportportal.testng.TestNGService.ITEM_TREE;
import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class CallbackReportingTest {

	@Test
	public void firstTest() {
		throw new RuntimeException();
	}

	@Test
	public void secondTest() {

	}

	@AfterMethod
	public void after(ITestResult testResult) {

		ItemTreeUtils.retrieveLeaf(testResult, ITEM_TREE).ifPresent(itemLeaf -> {
			if ("firstTest".equals(testResult.getName())) {
				sendFinishRequest(itemLeaf, "PASSED", "firstTest");
			}

			if ("secondTest".equals(testResult.getName())) {
				sendFinishRequest(itemLeaf, "FAILED", "secondTest");
				attachLog(itemLeaf);
			}
		});

	}

	private void sendFinishRequest(TestItemTree.TestItemLeaf testResultLeaf, String status, String description) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setDescription(description);
		finishTestItemRQ.setStatus(status);
		finishTestItemRQ.setEndTime(Calendar.getInstance().getTime());
		ItemTreeReporter.finishItem(TestNGService.getReportPortal().getClient(), finishTestItemRQ, ITEM_TREE.getLaunchId(), testResultLeaf)
				.cache()
				.blockingGet();
	}

	private void attachLog(TestItemTree.TestItemLeaf testItemLeaf) {
		ofNullable(Launch.currentLaunch()).ifPresent(l -> ItemTreeReporter.sendLog(l.getClient(),
				"ERROR",
				"Error message",
				Calendar.getInstance().getTime(),
				ITEM_TREE.getLaunchId(),
				testItemLeaf
		));
	}
}
