/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.aura.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.mcp.service.SeleniumErrorDistiller;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.ReportDifferentialDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TestTransitionDto;
import com.xceptance.aura.report.dto.TestTransitionDto.TransitionType;
import com.xceptance.aura.report.entity.TestRunEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ReportDifferentialService}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class ReportDifferentialServiceTest
{
    private AuraReportDataService mockDataService;
    private SeleniumErrorDistiller errorDistiller;
    private ReportDifferentialService service;

    @BeforeEach
    public void setUp()
    {
        mockDataService = Mockito.mock(AuraReportDataService.class);
        errorDistiller = new SeleniumErrorDistiller();
        service = new ReportDifferentialService(mockDataService, null, errorDistiller);
    }

    @Test
    public void testEmptyInputsReturnsZeroDifferential()
    {
        final ReportDifferentialDto result = service.computeDifferential(Collections.emptyList());

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getTotalTransitions());
        Assertions.assertEquals(0, result.getTurnedGreenCount());
        Assertions.assertEquals(0, result.getUnticketedFailureCount());
        Assertions.assertEquals(0, result.getKnownFailureCount());
        Assertions.assertEquals(0, result.getRegressionCount());
        Assertions.assertEquals(0, result.getStablePassCount());
        Assertions.assertTrue(result.getTurnedGreen().isEmpty());
        Assertions.assertTrue(result.getUnticketedFailures().isEmpty());
        Assertions.assertTrue(result.getKnownFailures().isEmpty());
        Assertions.assertTrue(result.getRegressions().isEmpty());
        Assertions.assertTrue(result.getAllTransitions().isEmpty());
    }

    @Test
    public void testChronologicalMultiRunTransitions()
    {
        final String runIdOld = "run-20261120-100000";
        final String runIdNew = "run-20261121-100000";

        final TestRunEntity runOld = createRun(runIdOld, 1732093200000L);
        final TestRunEntity runNew = createRun(runIdNew, 1732179600000L);

        // Run Old (Oldest):
        // Test A: Fails (no bug)
        // Test B: Fails (has bug BUG-101)
        // Test C: Passes
        // Test D: Passes
        final List<TestExecutionDto> oldExecutions = List.of(
            createExecution("e-a1", runIdOld, "com.example.OrderTest", "testA", "Order Test A", "failed",
                "org.openqa.selenium.NoSuchElementException: Cannot locate element with id: submit-order", Collections.emptyList()),
            createExecution("e-b1", runIdOld, "com.example.OrderTest", "testB", "Order Test B", "failed",
                "org.openqa.selenium.StaleElementReferenceException: stale element", List.of("BUG-101")),
            createExecution("e-c1", runIdOld, "com.example.OrderTest", "testC", "Order Test C", "passed",
                null, Collections.emptyList()),
            createExecution("e-d1", runIdOld, "com.example.OrderTest", "testD", "Order Test D", "passed",
                null, Collections.emptyList())
        );

        // Run New (Newest):
        // Test A: Passes -> TURNED_GREEN (was red in runOld, now green)
        // Test B: Fails with bug BUG-101 -> KNOWN_FAIL
        // Test C: Fails without bug -> REGRESSION (was green in runOld, now red)
        // Test D: Passes -> STABLE_PASS
        // Test E: New test fails without bug -> UNTICKETED_FAIL
        final List<TestExecutionDto> newExecutions = List.of(
            createExecution("e-a2", runIdNew, "com.example.OrderTest", "testA", "Order Test A", "passed",
                null, Collections.emptyList()),
            createExecution("e-b2", runIdNew, "com.example.OrderTest", "testB", "Order Test B", "failed",
                "org.openqa.selenium.StaleElementReferenceException: stale element", List.of("BUG-101")),
            createExecution("e-c2", runIdNew, "com.example.OrderTest", "testC", "Order Test C", "failed",
                "org.openqa.selenium.TimeoutException: Timed out waiting for cart", Collections.emptyList()),
            createExecution("e-d2", runIdNew, "com.example.OrderTest", "testD", "Order Test D", "passed",
                null, Collections.emptyList()),
            createExecution("e-e2", runIdNew, "com.example.OrderTest", "testE", "Order Test E", "failed",
                "org.openqa.selenium.NoSuchElementException: Element not found: By.cssSelector: #pay-button", Collections.emptyList())
        );

        Mockito.when(mockDataService.getRunReport(runIdOld)).thenReturn(createRunReport(runIdOld, oldExecutions));
        Mockito.when(mockDataService.getRunReport(runIdNew)).thenReturn(createRunReport(runIdNew, newExecutions));

        final ReportDifferentialDto diff = service.computeDifferential(List.of(runNew, runOld));

        Assertions.assertNotNull(diff);
        Assertions.assertEquals(5, diff.getTotalTransitions());
        Assertions.assertEquals(1, diff.getTurnedGreenCount());
        Assertions.assertEquals(1, diff.getUnticketedFailureCount());
        Assertions.assertEquals(1, diff.getKnownFailureCount());
        Assertions.assertEquals(1, diff.getRegressionCount());
        Assertions.assertEquals(1, diff.getStablePassCount());

        // Check Turned Green (Test A)
        final TestTransitionDto turnedGreen = diff.getTurnedGreen().get(0);
        Assertions.assertEquals("testA", turnedGreen.getTestMethod());
        Assertions.assertEquals(TransitionType.TURNED_GREEN, turnedGreen.getTransition());
        Assertions.assertEquals("passed", turnedGreen.getLatestStatus());
        Assertions.assertEquals(runIdNew, turnedGreen.getLatestRunId());
        Assertions.assertEquals(runIdOld, turnedGreen.getPriorFailedRunId());
        Assertions.assertEquals("failed", turnedGreen.getPriorFailedStatus());

        // Check Known Fail (Test B)
        final TestTransitionDto knownFail = diff.getKnownFailures().get(0);
        Assertions.assertEquals("testB", knownFail.getTestMethod());
        Assertions.assertEquals(TransitionType.KNOWN_FAIL, knownFail.getTransition());
        Assertions.assertTrue(knownFail.hasBugs());
        Assertions.assertEquals("BUG-101", knownFail.getBugTickets().get(0));

        // Check Regression (Test C)
        final TestTransitionDto regression = diff.getRegressions().get(0);
        Assertions.assertEquals("testC", regression.getTestMethod());
        Assertions.assertEquals(TransitionType.REGRESSION, regression.getTransition());
        Assertions.assertEquals("failed", regression.getLatestStatus());
        Assertions.assertFalse(regression.hasBugs());

        // Check Unticketed Fail (Test E)
        final TestTransitionDto unticketed = diff.getUnticketedFailures().get(0);
        Assertions.assertEquals("testE", unticketed.getTestMethod());
        Assertions.assertEquals(TransitionType.UNTICKETED_FAIL, unticketed.getTransition());
        Assertions.assertFalse(unticketed.hasBugs());
        Assertions.assertNotNull(unticketed.getErrorCategory());
        Assertions.assertFalse(unticketed.getErrorCategory().isBlank());
        Assertions.assertTrue(unticketed.getLocator() != null && unticketed.getLocator().contains("pay-button"));
    }

    @Test
    public void testFallbackVariationsHandling()
    {
        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k1", "succeeded-fixed", true, "JAVA", "run-1", "Default", "Chrome", "US", null, "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k2", "failed-unknown", false, "JAVA", "run-1", "Default", "Chrome", "US", null, "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k3", "k3", "failed-known", false, "JAVA", "run-1", "Default", "Chrome", "US", null, "Default", Collections.emptyMap(), 0.0, 0L, "exec-3", List.of("BUG-555")),
            new BatchLatestVariationDto("k4", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", null, "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final ReportDifferentialDto diff = service.computeDifferential(Collections.emptyList(), variations, null);

        Assertions.assertNotNull(diff);
        Assertions.assertEquals(4, diff.getTotalTransitions());
        Assertions.assertEquals(1, diff.getTurnedGreenCount());
        Assertions.assertEquals(1, diff.getUnticketedFailureCount());
        Assertions.assertEquals(1, diff.getKnownFailureCount());
        Assertions.assertEquals(1, diff.getStablePassCount());

        Assertions.assertEquals(TransitionType.TURNED_GREEN, diff.getTurnedGreen().get(0).getTransition());
        Assertions.assertEquals(TransitionType.UNTICKETED_FAIL, diff.getUnticketedFailures().get(0).getTransition());
        Assertions.assertEquals(TransitionType.KNOWN_FAIL, diff.getKnownFailures().get(0).getTransition());
        Assertions.assertEquals("BUG-555", diff.getKnownFailures().get(0).getBugTickets().get(0));
    }

    @Test
    public void testPrimaryBugFallbackPreservedInKnownFailure()
    {
        final String runId = "run-20261124-100000";
        final TestRunEntity run = createRun(runId, 1732438800000L);
        final TestExecutionDto exec = Mockito.spy(createExecution(
            "e-p1", runId, "com.example.OrderTest", "testPrimaryBug", "Primary Bug Test",
            "failed", "AssertionError: mismatch", Collections.emptyList()
        ));
        Mockito.doReturn("BUG-999").when(exec).getPrimaryBug();
        Mockito.when(mockDataService.getRunReport(runId)).thenReturn(createRunReport(runId, List.of(exec)));

        final ReportDifferentialDto diff = service.computeDifferential(List.of(run));

        Assertions.assertEquals(1, diff.getKnownFailureCount());
        final TestTransitionDto known = diff.getKnownFailures().get(0);
        Assertions.assertEquals(TransitionType.KNOWN_FAIL, known.getTransition());
        Assertions.assertEquals(List.of("BUG-999"), known.getBugs());
    }

    @Test
    public void testSameRunRetryNotMisclassifiedAsMultiRunTurnedGreen()
    {
        final String runId = "run-20261124-100100";
        final TestRunEntity run = createRun(runId, 1732438860000L);
        final List<TestExecutionDto> execs = List.of(
            createExecution("e-1a", runId, "com.example.RetryTest", "testRetry", "Retry Test", "failed",
                "org.openqa.selenium.TimeoutException: slow load", Collections.emptyList()),
            createExecution("e-1b", runId, "com.example.RetryTest", "testRetry", "Retry Test", "passed",
                null, Collections.emptyList())
        );
        Mockito.when(mockDataService.getRunReport(runId)).thenReturn(createRunReport(runId, execs));

        final ReportDifferentialDto diff = service.computeDifferential(List.of(run));

        Assertions.assertEquals(1, diff.getTotalEvaluatedTests());
        Assertions.assertEquals(0, diff.getTurnedGreenCount());
        Assertions.assertEquals(1, diff.getStablePassCount());
        Assertions.assertEquals("", diff.getAllTransitions().get(0).getPreviousFailedRunId());
    }

    @Test
    public void testRegressionPrecedenceAndHealedWithFailureText()
    {
        final String run1 = "run-1";
        final String run2 = "run-2";
        final String run3 = "run-3";

        Mockito.when(mockDataService.getRunReport(run1)).thenReturn(createRunReport(run1, List.of(
            createExecution("e1-known", run1, "com.example.T", "mKnown", "Known", "passed", null, Collections.emptyList()),
            createExecution("e1-flap", run1, "com.example.T", "mFlap", "Flap", "failed", "err1", Collections.emptyList()),
            createExecution("e1-heal", run1, "com.example.T", "mHeal", "Heal", "failed", "err2", Collections.emptyList())
        )));
        Mockito.when(mockDataService.getRunReport(run2)).thenReturn(createRunReport(run2, List.of(
            createExecution("e2-known", run2, "com.example.T", "mKnown", "Known", "passed", null, Collections.emptyList()),
            createExecution("e2-flap", run2, "com.example.T", "mFlap", "Flap", "passed", null, Collections.emptyList()),
            createExecution("e2-heal", run2, "com.example.T", "mHeal", "Heal", "passed", null, Collections.emptyList())
        )));
        Mockito.when(mockDataService.getRunReport(run3)).thenReturn(createRunReport(run3, List.of(
            createExecution("e3-known", run3, "com.example.T", "mKnown", "Known", "failed", "err", List.of("BUG-42")),
            createExecution("e3-flap", run3, "com.example.T", "mFlap", "Flap", "failed", "err3", Collections.emptyList()),
            createExecution("e3-heal", run3, "com.example.T", "mHeal", "Heal", "succeeded-fixed", "prior failure note", Collections.emptyList())
        )));

        final ReportDifferentialDto diff = service.computeDifferential(List.of(
            createRun(run1, 1000L), createRun(run2, 2000L), createRun(run3, 3000L)
        ));

        Assertions.assertEquals(1, diff.getKnownFailureCount());
        Assertions.assertEquals("mKnown", diff.getKnownFailures().get(0).getTestMethod());
        Assertions.assertEquals(1, diff.getRegressionCount());
        Assertions.assertEquals("mFlap", diff.getRegressions().get(0).getTestMethod());
        Assertions.assertEquals(1, diff.getTurnedGreenCount());
        Assertions.assertEquals("mHeal", diff.getTurnedGreen().get(0).getTestMethod());
    }

    @Test
    public void testJsonSerializationHasNoDuplicateAliasFields() throws Exception
    {
        final TestTransitionDto item = new TestTransitionDto(
            "k1", "v1", "com.example.Test", "m1", "Title", "Chrome", "US",
            TransitionType.UNTICKETED_FAIL, "failed", "run-2", "exec-2", "run-1", "failed",
            List.of("BUG-1"), List.of("HIGH"), "TIMEOUT", "trace", "#btn", List.of("Chrome")
        );
        final ReportDifferentialDto dto = new ReportDifferentialDto(
            List.of("run-1", "run-2"), 1, 0, 1, 0, 0, 0,
            Collections.emptyList(), List.of(item), Collections.emptyList(), Collections.emptyList(), List.of(item)
        );

        final JsonNode root = new ObjectMapper().valueToTree(dto);
        Assertions.assertTrue(root.has("analyzedRunIds"));
        Assertions.assertFalse(root.has("runIds"));
        Assertions.assertTrue(root.has("totalEvaluatedTests"));
        Assertions.assertFalse(root.has("totalVariations"));
        Assertions.assertFalse(root.has("totalTransitions"));
        Assertions.assertTrue(root.has("unticketedFailureCount"));
        Assertions.assertFalse(root.has("unticketedFailCount"));
        Assertions.assertTrue(root.has("knownFailureCount"));
        Assertions.assertFalse(root.has("knownFailCount"));

        final JsonNode child = root.get("unticketedFailures").get(0);
        Assertions.assertTrue(child.has("previousFailedRunId"));
        Assertions.assertFalse(child.has("previousRunId"));
        Assertions.assertFalse(child.has("priorFailedRunId"));
        Assertions.assertTrue(child.has("bugs"));
        Assertions.assertFalse(child.has("bugTickets"));
        Assertions.assertTrue(child.has("errorCategory"));
        Assertions.assertFalse(child.has("distilledErrorCategory"));
        Assertions.assertTrue(child.has("distilledError"));
        Assertions.assertFalse(child.has("cleanedStackTrace"));
        Assertions.assertTrue(child.has("locator"));
        Assertions.assertFalse(child.has("distilledLocator"));
    }

    @Test
    public void testComputeBatchDifferentialDataServiceFallbackFiltersByBatchAndDeleted()
    {
        final TestRunEntity deleted = createRun("run-del", 3000L);
        deleted.setBatchName("BatchA");
        deleted.setIsDeleted(true);

        final TestRunEntity batchB = createRun("run-b", 2000L);
        batchB.setBatchName("BatchB");

        final TestRunEntity batchA = createRun("run-a", 1000L);
        batchA.setBatchName("BatchA");

        Mockito.when(mockDataService.getAllRuns()).thenReturn(List.of(deleted, batchB, batchA));
        Mockito.when(mockDataService.getRunReport("run-a")).thenReturn(createRunReport("run-a", List.of(
            createExecution("e-a", "run-a", "com.example.T", "mA", "A", "passed", null, Collections.emptyList())
        )));

        final ReportDifferentialDto diff = service.computeBatchDifferential("BatchA", 5);

        Assertions.assertEquals(List.of("run-a"), diff.getAnalyzedRunIds());
        Assertions.assertEquals(1, diff.getStablePassCount());
    }

    private TestRunEntity createRun(final String runId, final long startTimeMs)
    {
        final TestRunEntity run = new TestRunEntity();
        run.setId(runId);
        run.setStartTimeMs(startTimeMs);
        return run;
    }

    private TestExecutionDto createExecution(
        final String id,
        final String runId,
        final String testClass,
        final String testMethod,
        final String title,
        final String status,
        final String failure,
        final List<String> bugs)
    {
        return new TestExecutionDto(
            id, runId, testClass, testMethod, title,
            testClass + "." + testMethod, "", "",
            status, "Java", "US", "Chrome",
            failure, bugs, "", "Default",
            Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(),
            null, null
        );
    }

    private RunReportDto createRunReport(final String runId, final List<TestExecutionDto> executions)
    {
        return new RunReportDto(
            runId, "DefaultBatch", "production", "2026-11-23 10:00:00", "1m",
            executions.size(), 0, 0, 0, 0, 0, 0,
            executions, new ArrayList<>(), 0, 0L, 0.0, Collections.emptyMap()
        );
    }
}
