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

import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit tests verifying that Test Base variation status is accurately synchronized
 * with active bugs and history runs across ingestion, bug linking/unlinking, and page data assembly.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class TestBaseVariationStatusSyncTest
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
    public void testGetTestBaseDataReevaluatesFailedUnknownToFailedKnownWhenActiveBugExists()
    {
        final String testClass = "com.xceptance.neodymium.test.HomepageTest";
        final String testMethod = "testHomepage";
        final String dataSet = "canyon_homepage_US";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, testMethod, dataSet, "@Browsing", location, browser);
        varEntity.setLastStatus("failed-unknown");

        final TestBaseBugEntity activeBug = new TestBaseBugEntity(varId, "NEO-1234", "ALL", "ALL", System.currentTimeMillis(), "RUN_1");

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(varEntity));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of(activeBug));

        final TestBaseDataDto testBaseData = dataService.getTestBaseData();

        Assertions.assertNotNull(testBaseData);
        final List<TestBaseAreaDto> areas = testBaseData.getAreas();
        Assertions.assertEquals(1, areas.size());

        final List<TestBaseClassDto> classes = areas.get(0).getTestClasses();
        Assertions.assertEquals(1, classes.size());

        final List<TestBaseVariationEntity> variations = classes.get(0).getVariations();
        Assertions.assertEquals(1, variations.size());
        Assertions.assertEquals("failed-known", variations.get(0).getLastStatus());

        final ArgumentCaptor<TestBaseVariationEntity> captor = ArgumentCaptor.forClass(TestBaseVariationEntity.class);
        Mockito.verify(variationRepository, Mockito.atLeastOnce()).save(captor.capture());
        Assertions.assertEquals("failed-known", captor.getValue().getLastStatus());
    }

    @Test
    public void testIngestExecutionUpdatesVariationLastStatusToFailedKnownWhenBugPresent()
    {
        final String runId = "RUN_INGEST_1";
        final String testClass = "HomepageTest";
        final String testMethod = "testHomepage";
        final String dataSet = "canyon_homepage_US";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, testMethod, dataSet, "@Browsing", location, browser);
        varEntity.setLastStatus("failed-unknown");

        final TestRunEntity runEntity = new TestRunEntity(
            runId, "ALL", "RUNNING", "ALL", "ALL", "Java", browser, "Recently", System.currentTimeMillis()
        );
        Mockito.when(runRepository.findById(runId)).thenReturn(Optional.of(runEntity));

        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));

        final TestBaseBugEntity activeBug = new TestBaseBugEntity(varId, "NEO-555", "ALL", "ALL", System.currentTimeMillis(), runId);
        Mockito.when(bugRepository.findByVariationIdAndBatchNameInAndEnvironmentIn(
            Mockito.eq(varId), Mockito.anyList(), Mockito.anyList()
        )).thenReturn(List.of(activeBug));

        final Map<String, Object> payload = Map.of(
            "testClass", testClass,
            "testMethod", testMethod,
            "title", dataSet,
            "location", location,
            "browser", browser,
            "status", "failed",
            "areaName", "Browsing"
        );

        dataService.ingestExecution(runId, payload);

        final ArgumentCaptor<TestBaseVariationEntity> captor = ArgumentCaptor.forClass(TestBaseVariationEntity.class);
        Mockito.verify(variationRepository, Mockito.atLeastOnce()).save(captor.capture());
        Assertions.assertEquals("failed-known", captor.getValue().getLastStatus());
    }
}
