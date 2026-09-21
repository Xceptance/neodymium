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

    @Test
    public void testGetTestBaseDataGroupsAllClassVariationsUnderSingleClassAndExcludesBlankMethodEntries()
    {
        final String class1 = "com.xceptance.neodymium.test.CheckoutTest";
        final String class2 = "com.xceptance.neodymium.test.SearchTest";

        final TestBaseVariationEntity var1 = new TestBaseVariationEntity(
            "v1", class1, "testCart", "dataset_1", "@Checkout", "US", "Chrome"
        );
        var1.setLastStatus("passed");

        final TestBaseVariationEntity var2 = new TestBaseVariationEntity(
            "v2", class1, "testPayment", "dataset_2", "@General", "US", "Firefox"
        );
        var2.setLastStatus("passed");

        // Pseudo-variation representing class/dataset container execution (blank testMethodName)
        final TestBaseVariationEntity pseudoVar1 = new TestBaseVariationEntity(
            "v3", class1, "", "dataset_1", "@Checkout", "US", "Chrome"
        );

        final TestBaseVariationEntity pseudoVar2 = new TestBaseVariationEntity(
            "v4", class1, null, "Default", "@Checkout", "US", "Chrome"
        );

        final TestBaseVariationEntity var3 = new TestBaseVariationEntity(
            "v5", class2, "testSearch", "dataset_1", "@Browsing", "US", "Chrome"
        );
        var3.setLastStatus("passed");

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc())
            .thenReturn(List.of(var1, var2, pseudoVar1, pseudoVar2, var3));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        final TestBaseDataDto testBaseData = dataService.getTestBaseData();

        Assertions.assertNotNull(testBaseData);
        Assertions.assertEquals(2, testBaseData.getTotalTestClassesCount());
        Assertions.assertEquals(3, testBaseData.getTotalVariationsCount());

        final List<TestBaseAreaDto> areas = testBaseData.getAreas();
        Assertions.assertEquals(2, areas.size());

        final Optional<TestBaseAreaDto> checkoutAreaOpt = areas.stream()
            .filter(a -> "@Checkout".equalsIgnoreCase(a.getAreaName()))
            .findFirst();
        Assertions.assertTrue(checkoutAreaOpt.isPresent());

        final List<TestBaseClassDto> checkoutClasses = checkoutAreaOpt.get().getTestClasses();
        Assertions.assertEquals(1, checkoutClasses.size());

        final TestBaseClassDto checkoutClassDto = checkoutClasses.get(0);
        Assertions.assertEquals(class1, checkoutClassDto.getClassName());
        Assertions.assertEquals(2, checkoutClassDto.getVariationCount());
        Assertions.assertEquals(2, checkoutClassDto.getVariations().size());
        Assertions.assertEquals("testCart", checkoutClassDto.getVariations().get(0).getTestMethodName());
        Assertions.assertEquals("testPayment", checkoutClassDto.getVariations().get(1).getTestMethodName());
    }

    @Test
    public void testNormalizeLocationAndDeduplicateVariations()
    {
        Assertions.assertEquals("Unknown", AuraReportDataService.normalizeLocation("UNKNOWN"));
        Assertions.assertEquals("Unknown", AuraReportDataService.normalizeLocation("unknown"));
        Assertions.assertEquals("Unknown", AuraReportDataService.normalizeLocation("Unknown"));
        Assertions.assertEquals("Unknown", AuraReportDataService.normalizeLocation(null));
        Assertions.assertEquals("US", AuraReportDataService.normalizeLocation("us"));
        Assertions.assertEquals("DE", AuraReportDataService.normalizeLocation("de"));

        final String class1 = "Aura_stokke_test_yml_yaml_Test";
        final TestBaseVariationEntity varUpper = new TestBaseVariationEntity(
            "v_upper", class1, "executeYamlTest", "Default", "@General", "UNKNOWN", "Chrome_1920x1080"
        );
        varUpper.setTotalExecutionsCount(6);
        varUpper.setHistoryLinks("/run-report?runId=R1");

        final TestBaseVariationEntity varMixed = new TestBaseVariationEntity(
            "v_mixed", class1, "executeYamlTest", "Default", "@General", "Unknown", "Chrome_1920x1080"
        );
        varMixed.setTotalExecutionsCount(4);
        varMixed.setHistoryLinks("/run-report?runId=R2");

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc())
            .thenReturn(List.of(varUpper, varMixed));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        final TestBaseDataDto testBaseData = dataService.getTestBaseData();

        Assertions.assertNotNull(testBaseData);
        Assertions.assertEquals(1, testBaseData.getTotalTestClassesCount());
        Assertions.assertEquals(1, testBaseData.getTotalVariationsCount());

        final List<TestBaseVariationEntity> vars = testBaseData.getAreas().get(0).getTestClasses().get(0).getVariations();
        Assertions.assertEquals(1, vars.size());
        Assertions.assertEquals("Unknown", vars.get(0).getLocation());
        Assertions.assertEquals(2, vars.get(0).getTotalExecutionsCount());
        Assertions.assertTrue(vars.get(0).getHistoryLinks().contains("R1"));
        Assertions.assertTrue(vars.get(0).getHistoryLinks().contains("R2"));

        Mockito.verify(variationRepository, Mockito.atLeastOnce()).deleteAll(Mockito.anyList());
    }

    @Test
    public void testCountHistoryLinksAndRecalibrateExecutionCounts()
    {
        final String history = "/run-report?runId=R1,/run-report?runId=R2,/run-report?runId=R3,/run-report?runId=R4,/run-report?runId=R5";
        Assertions.assertEquals(5, AuraReportDataService.countHistoryLinks(history));
        Assertions.assertEquals(0, AuraReportDataService.countHistoryLinks(null));

        final String class1 = "Aura_tests_examples_wikipedia_search_yml_Test";
        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(
            "v_wiki_1", class1, "executeYamlTest", "wikipedia_search_example_1", "@Browsing", "Unknown", "Chrome_1920x1080"
        );
        varEntity.setTotalExecutionsCount(14);
        varEntity.setHistoryLinks(history);

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc())
            .thenReturn(List.of(varEntity));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        final TestBaseDataDto testBaseData = dataService.getTestBaseData();

        Assertions.assertNotNull(testBaseData);
        final List<TestBaseVariationEntity> vars = testBaseData.getAreas().get(0).getTestClasses().get(0).getVariations();
        Assertions.assertEquals(1, vars.size());
        Assertions.assertEquals(5, vars.get(0).getTotalExecutionsCount());
    }
}
