/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit tests verifying that removing runs cleans physical storage, marks runs as deleted,
 * updates batch latest pointers, purges variation execution history, updates the last variation
 * status and timestamp when the last run is removed, and reconciles bug entities under Option B.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraReportRunRemovalTest
{
    private TestRunRepository runRepository;
    private TestBatchRepository batchRepository;
    private TestBaseVariationRepository variationRepository;
    private TestBaseBugRepository bugRepository;
    private LocalRunJsonStorageService storageService;
    private AttachmentStorageService attachmentStorageService;
    private AuraReportDataService dataService;

    @BeforeEach
    public void setUp()
    {
        runRepository = Mockito.mock(TestRunRepository.class);
        batchRepository = Mockito.mock(TestBatchRepository.class);
        variationRepository = Mockito.mock(TestBaseVariationRepository.class);
        bugRepository = Mockito.mock(TestBaseBugRepository.class);
        storageService = Mockito.mock(LocalRunJsonStorageService.class);
        attachmentStorageService = Mockito.mock(AttachmentStorageService.class);

        dataService = new AuraReportDataService(
            runRepository,
            batchRepository,
            variationRepository,
            bugRepository,
            storageService,
            attachmentStorageService
        );
    }

    @Test
    public void testRemoveRunFromHistorySoftDeletesRunAndDeletesStorage()
    {
        final String runId = "RUN_100";
        final TestRunEntity runEntity = new TestRunEntity(
            runId, "CheckoutSuite", "COMPLETED", "CI", "Staging", "en_US", "Chrome", "10:00", 1000L
        );
        Mockito.when(runRepository.findById(runId)).thenReturn(Optional.of(runEntity));
        Mockito.when(variationRepository.findAll()).thenReturn(List.of());
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        dataService.removeRunFromHistory(runId);

        final ArgumentCaptor<TestRunEntity> runCaptor = ArgumentCaptor.forClass(TestRunEntity.class);
        Mockito.verify(runRepository).save(runCaptor.capture());
        Assertions.assertTrue(runCaptor.getValue().getIsDeleted());

        Mockito.verify(storageService).deleteRunJson(runId);
        Mockito.verify(attachmentStorageService).deleteRunAttachments(runId);
    }

    @Test
    public void testRemoveRunFromHistoryUpdatesBatchLatestRunId()
    {
        final String runId2 = "RUN_2";
        final String runId1 = "RUN_1";
        final String batchName = "SmokeTests";

        final TestRunEntity run2 = new TestRunEntity(
            runId2, batchName, "COMPLETED", "CI", "Staging", "en_US", "Chrome", "11:00", 2000L
        );
        final TestRunEntity run1 = new TestRunEntity(
            runId1, batchName, "COMPLETED", "CI", "Staging", "en_US", "Chrome", "10:00", 1000L
        );

        final TestBatchEntity batchEntity = new TestBatchEntity(batchName, "Staging", "Smoke test suite", "en_US", "Chrome", runId2);

        Mockito.when(runRepository.findById(runId2)).thenReturn(Optional.of(run2));
        Mockito.when(batchRepository.findById(batchName)).thenReturn(Optional.of(batchEntity));
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName)).thenReturn(List.of(run1));
        Mockito.when(variationRepository.findAll()).thenReturn(List.of());
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        dataService.removeRunFromHistory(runId2);

        final ArgumentCaptor<TestBatchEntity> batchCaptor = ArgumentCaptor.forClass(TestBatchEntity.class);
        Mockito.verify(batchRepository).save(batchCaptor.capture());
        Assertions.assertEquals(runId1, batchCaptor.getValue().getLatestRunId());
    }

    @Test
    public void testRemoveRunFromHistoryUpdatesVariationWhenDeletedRunWasLastRun()
    {
        final String runId2 = "RUN_2";
        final String runId1 = "RUN_1";
        final String batchName = "PaymentBatch";

        final TestRunEntity run2 = new TestRunEntity(runId2, batchName, "COMPLETED", "CI", "Prod", "en_US", "Chrome", "11:00", 2000L);
        final TestRunEntity run1 = new TestRunEntity(runId1, batchName, "COMPLETED", "CI", "Prod", "en_US", "Chrome", "10:00", 1000L);

        Mockito.when(runRepository.findById(runId2)).thenReturn(Optional.of(run2));
        Mockito.when(runRepository.findById(runId1)).thenReturn(Optional.of(run1));

        final String testClass = "com.xceptance.neodymium.test.PaymentTest";
        final String testMethod = "testCreditCard";
        final String dataSet = "Visa";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);

        final TestBaseVariationEntity variation = new TestBaseVariationEntity(
            varId, testClass, testMethod, dataSet, "@Payment", location, browser
        );

        final String link2 = "/run-report?runId=" + runId2 + "&executionId=e2&batch=" + batchName + "&ts=11%3A00&status=failed-unknown";
        final String link1 = "/run-report?runId=" + runId1 + "&executionId=e1&batch=" + batchName + "&ts=10%3A00&status=passed-clean";
        variation.setHistoryLinks(link2 + "," + link1);
        variation.setTotalExecutionsCount(2);
        variation.setLastStatus("failed-unknown");
        variation.setLastExecutedAt(2000L);
        variation.setHealed(false);

        Mockito.when(variationRepository.findAll()).thenReturn(List.of(variation));
        Mockito.when(bugRepository.findByVariationId(varId)).thenReturn(List.of());
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        dataService.removeRunFromHistory(runId2);

        final ArgumentCaptor<TestBaseVariationEntity> varCaptor = ArgumentCaptor.forClass(TestBaseVariationEntity.class);
        Mockito.verify(variationRepository).save(varCaptor.capture());
        final TestBaseVariationEntity savedVar = varCaptor.getValue();

        Assertions.assertEquals(1, savedVar.getTotalExecutionsCount());
        Assertions.assertEquals(link1, savedVar.getHistoryLinks());
        Assertions.assertEquals("passed-clean", savedVar.getLastStatus());
        Assertions.assertEquals(1000L, savedVar.getLastExecutedAt());
        Assertions.assertFalse(savedVar.isHealed());
    }

    @Test
    public void testRemoveRunFromHistoryPrunesEmptyVariationWhenAllRunsDeleted()
    {
        final String runId = "RUN_SOLO";
        final TestRunEntity run = new TestRunEntity(runId, "SoloBatch", "COMPLETED", "CI", "Prod", "en_US", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findById(runId)).thenReturn(Optional.of(run));

        final String testClass = "com.xceptance.neodymium.test.SoloTest";
        final String testMethod = "testSolo";
        final String dataSet = "Default";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);

        final TestBaseVariationEntity variation = new TestBaseVariationEntity(
            varId, testClass, testMethod, dataSet, "@Solo", location, browser
        );
        final String link = "/run-report?runId=" + runId + "&executionId=e1&batch=SoloBatch&ts=10%3A00&status=passed-clean";
        variation.setHistoryLinks(link);
        variation.setTotalExecutionsCount(1);
        variation.setLastStatus("passed-clean");
        variation.setLastExecutedAt(1000L);

        final TestBaseBugEntity soloBug = new TestBaseBugEntity(varId, "NEO-999", "SoloBatch", "Prod", 1000L, runId);

        Mockito.when(variationRepository.findAll()).thenReturn(List.of(variation));
        Mockito.when(bugRepository.findByVariationId(varId)).thenReturn(List.of(soloBug));
        Mockito.when(bugRepository.findAll()).thenReturn(new ArrayList<>(List.of(soloBug)));

        dataService.removeRunFromHistory(runId);

        Mockito.verify(variationRepository).deleteAll(Mockito.argThat(list -> 
            java.util.stream.StreamSupport.stream(list.spliterator(), false).anyMatch(v -> v.equals(variation))
        ));
        Mockito.verify(bugRepository).deleteAll(Mockito.argThat(list -> 
            java.util.stream.StreamSupport.stream(list.spliterator(), false).anyMatch(b -> b.equals(soloBug))
        ));
    }

    @Test
    public void testRemoveRunFromHistoryReconcilesBugsUnderOptionB()
    {
        final String runId = "RUN_RECONCILE";
        final TestRunEntity run = new TestRunEntity(runId, "ReconcileBatch", "COMPLETED", "CI", "Prod", "en_US", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        Mockito.when(variationRepository.findAll()).thenReturn(List.of());

        final TestBaseBugEntity linkedBug = new TestBaseBugEntity("v1", "NEO-111", "ReconcileBatch", "Prod", 1000L, runId);
        final TestBaseBugEntity unlinkedBug = new TestBaseBugEntity("v2", "NEO-222", "ReconcileBatch", "Prod", 1000L, "OTHER_RUN");
        unlinkedBug.setRemovedRunId(runId);
        unlinkedBug.setRemovedAtMs(1000L);

        Mockito.when(bugRepository.findAll()).thenReturn(List.of(linkedBug, unlinkedBug));

        dataService.removeRunFromHistory(runId);

        final ArgumentCaptor<TestBaseBugEntity> bugCaptor = ArgumentCaptor.forClass(TestBaseBugEntity.class);
        Mockito.verify(bugRepository, Mockito.atLeast(2)).save(bugCaptor.capture());

        final List<TestBaseBugEntity> savedBugs = bugCaptor.getAllValues();
        final TestBaseBugEntity reconciledLinked = savedBugs.stream().filter(b -> "NEO-111".equals(b.getBugTicket())).findFirst().orElseThrow();
        Assertions.assertNull(reconciledLinked.getLinkedRunId(), "linkedRunId should be cleared under Option B");

        final TestBaseBugEntity reconciledUnlinked = savedBugs.stream().filter(b -> "NEO-222".equals(b.getBugTicket())).findFirst().orElseThrow();
        Assertions.assertNull(reconciledUnlinked.getRemovedRunId(), "removedRunId should be cleared to restore unlinked bug");
        Assertions.assertNull(reconciledUnlinked.getRemovedAtMs());
    }
}
