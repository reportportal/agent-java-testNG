/*
 * Copyright 2023 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.testng.bug;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.testng.BaseTestNGListener;
import com.epam.reportportal.testng.TestNGService;
import com.epam.reportportal.testng.integration.bug.SkipExceptionOnBeforeClassTest;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.testng.integration.util.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSkipExceptionOnBeforeClassTest {

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
    private final List<String> testUuidList =
            Arrays.asList(namedUuid("beforeClass"), namedUuid("beforeMethod"), namedUuid("test"),
                    namedUuid("afterMethod"), namedUuid("afterClass"));

    private final List<String> finishUuidOrder =
            Stream.concat(testUuidList.stream(), Stream.of(testClassUuid, suitedUuid)).collect(Collectors.toList());


    @Mock
    private ReportPortalClient client;

    @BeforeEach
    public void initMocks() {
        mockLaunch(client, namedUuid("launchUuid"), suitedUuid, testClassUuid, testUuidList);
        mockLogging(client);
        ReportPortal reportPortal = ReportPortal.create(client, standardParameters());
        TestListener.initReportPortal(reportPortal);
    }

    @Test
    public void verify_step_integrity_in_case_of_before_class_failed_with_skip_exception() {
        runTests(Collections.singletonList(TestListener.class), SkipExceptionOnBeforeClassTest.class);

        verify(client, times(1)).startLaunch(any()); // Start launch
        verify(client, times(1)).startTestItem(any());  // Start parent suites
        verify(client, times(1)).startTestItem(same(suitedUuid), any()); // Start test class

        ArgumentCaptor<StartTestItemRQ> startTestCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(client, times(5)).startTestItem(same(testClassUuid), startTestCapture.capture());
        List<StartTestItemRQ> startItems = startTestCapture.getAllValues();

        assertThat(startItems.get(0).getType(), equalTo(ItemType.BEFORE_CLASS.name()));
        assertThat(startItems.get(1).getType(), equalTo(ItemType.BEFORE_METHOD.name()));
        assertThat(startItems.get(2).getType(), equalTo(ItemType.STEP.name()));
        assertThat(startItems.get(3).getType(), equalTo(ItemType.AFTER_METHOD.name()));
        assertThat(startItems.get(4).getType(), equalTo(ItemType.AFTER_CLASS.name()));

        ArgumentCaptor<String> finishUuidCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<FinishTestItemRQ> finishItemCapture = ArgumentCaptor.forClass(FinishTestItemRQ.class);
        verify(client, times(finishUuidOrder.size())).finishTestItem(finishUuidCapture.capture(), finishItemCapture.capture());
        List<String> finishUuids = finishUuidCapture.getAllValues();
        assertThat(finishUuids, containsInAnyOrder(finishUuidOrder.toArray(new String[0])));

        List<FinishTestItemRQ> finishItems = finishItemCapture.getAllValues();

        FinishTestItemRQ beforeClassFinish = finishItems.get(0);
        assertThat(beforeClassFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
        assertThat(beforeClassFinish.getIssue(), nullValue());

        finishItems.subList(1, finishItems.size() - 2).forEach(finishTestItemRQ -> {
            assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
            assertThat(finishTestItemRQ.getIssue(), notNullValue());
            assertThat(finishTestItemRQ.getIssue().getIssueType(), equalTo(Launch.NOT_ISSUE.getIssueType()));
        });

        finishItems.subList(finishItems.size() - 2, finishItems.size()).forEach(finishTestItemRQ -> {
            assertThat(finishTestItemRQ.getStatus(), nullValue());
            assertThat(finishTestItemRQ.getIssue(), nullValue());
        });
    }
}
