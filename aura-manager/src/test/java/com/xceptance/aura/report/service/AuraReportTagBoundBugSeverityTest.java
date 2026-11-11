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

import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TestExecutionDto.BugDetailDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.repository.TestRunTagRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for bug severity and tag-bound bug linking in Aura reports.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportTagBoundBugSeverityTest
{
    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestBatchRepository batchRepository;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private TestBaseBugRepository bugRepository;

    @Autowired
    private TestBaseVariationRepository variationRepository;

    @Autowired
    private TestRunTagRepository tagRepository;

    @Autowired
    private LocalRunJsonStorageService storageService;

    private static final String BATCH = "TagBugBatch";
    private static final String RUN_SAFARI = "run-tag-safari";
    private static final String RUN_CHROME = "run-tag-chrome";
    private static final String RUN_MOBILE_IOS = "run-tag-mobile-ios";
    private static final String RUN_DESKTOP = "run-tag-desktop";

    @BeforeEach
    public void setUp() throws IOException
    {
        bugRepository.deleteAll();
        tagRepository.deleteAll();
        variationRepository.deleteAll();
        batchRepository.deleteAll();
        runRepository.deleteAll();

        batchRepository.save(new TestBatchEntity(BATCH, "Staging", "Tag Bug Batch", "US", "Safari", RUN_SAFARI));

        final long now = System.currentTimeMillis();

        // Failed execution on Safari with tags
        createRunAndReportWithTags(
            RUN_SAFARI, BATCH, now - 300000L, "row-safari-1", "failed", "Safari",
            Map.of("browser", List.of("Safari"), "os", List.of("macOS"))
        );

        // Failed execution on Chrome with tags (same test class & method)
        createRunAndReportWithTags(
            RUN_CHROME, BATCH, now - 200000L, "row-chrome-1", "failed", "Chrome",
            Map.of("browser", List.of("Chrome"), "os", List.of("Windows"))
        );

        // Failed execution on Mobile iOS
        createRunAndReportWithTags(
            RUN_MOBILE_IOS, BATCH, now - 100000L, "row-mobile-1", "failed", "Safari",
            Map.of("browser", List.of("Safari"), "os", List.of("iOS"), "device", List.of("iPhone"), "mobile", List.of())
        );

        // Failed execution on Desktop
        createRunAndReportWithTags(
            RUN_DESKTOP, BATCH, now - 50000L, "row-desktop-1", "failed", "Safari",
            Map.of("browser", List.of("Safari"), "os", List.of("macOS"), "desktop", List.of())
        );
    }

    private void createRunAndReportWithTags(
        final String runId,
        final String batchName,
        final long startTime,
        final String rowId,
        final String status,
        final String browser,
        final Map<String, List<String>> tags) throws IOException
    {
        final TestRunEntity runEntity = new TestRunEntity(
            runId, batchName, "COMPLETED", "Trigger", "Staging", "US", browser, "2026-10-28 10:00:00", startTime
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runRepository.save(runEntity);

        final TestExecutionDto exec = new TestExecutionDto(
            rowId, runId, "com.xceptance.neodymium.test.LoginTest", "testLogin",
            "LoginTest testLogin", status, browser, tags
        );

        final RunReportDto report = new RunReportDto(
            runId, batchName, "2026-10-28 10:00:00", "1.0s", 1,
            0, 0, 0, 1, 0, List.of(exec)
        );

        dataService.saveRunReportToDisk(runId, report);
    }

    @AfterEach
    public void tearDown() throws IOException
    {
        for (final String runId : List.of(RUN_SAFARI, RUN_CHROME, RUN_MOBILE_IOS, RUN_DESKTOP, "run-ingest-test", "run-stg-de", "run-stg-us", "run-prod-de", "run-dev-1"))
        {
            final Path runDir = storageService.getRunDir(runId);
            if (Files.exists(runDir))
            {
                try (final var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
            final Path flatFile = storageService.getRunJsonPath(runId);
            if (Files.exists(flatFile) && !Files.isDirectory(flatFile))
            {
                Files.deleteIfExists(flatFile);
            }
        }
    }

    @Test
    public void testBugSeverityAndTagBindingAttachedOnlyToMatchingRuns()
    {
        // Link bug with severity CRITICAL and bound to "browser: Safari"
        final TestExecutionDto result = dataService.addBugToExecution(
            RUN_SAFARI, "row-safari-1", "BUG-SAFARI-1", "CRITICAL", "browser: Safari"
        );

        // 1. Verify target execution has the bug, severity CRITICAL, and status failed-known
        Assertions.assertEquals(List.of("BUG-SAFARI-1"), result.getBugs());
        Assertions.assertEquals("failed-known", result.getStatus());
        Assertions.assertNotNull(result.getBugDetails());
        Assertions.assertEquals(1, result.getBugDetails().size());
        final BugDetailDto detail = result.getBugDetails().get(0);
        Assertions.assertEquals("BUG-SAFARI-1", detail.getBugTicket());
        Assertions.assertEquals("CRITICAL", detail.getSeverity());
        Assertions.assertEquals("browser: Safari", detail.getTagCriteria());

        // 2. Fetch report for Safari run: should have BUG-SAFARI-1 with status failed-known
        final RunReportDto reportSafari = dataService.getRunReport(RUN_SAFARI);
        final TestExecutionDto safariExec = reportSafari.getExecutions().get(0);
        Assertions.assertEquals(List.of("BUG-SAFARI-1"), safariExec.getBugs());
        Assertions.assertEquals("failed-known", safariExec.getStatus());
        Assertions.assertEquals(1, reportSafari.getKnownCount());
        Assertions.assertEquals(0, reportSafari.getUnknownCount());

        // 3. Fetch report for Chrome run: should NOT have BUG-SAFARI-1 and remain failed-unknown
        final RunReportDto reportChrome = dataService.getRunReport(RUN_CHROME);
        final TestExecutionDto chromeExec = reportChrome.getExecutions().get(0);
        Assertions.assertTrue(chromeExec.getBugs().isEmpty(), "Chrome run should not have bug bound to Safari");
        Assertions.assertEquals("failed-unknown", chromeExec.getStatus(), "Chrome status should remain failed-unknown");
        Assertions.assertEquals(0, reportChrome.getKnownCount());
        Assertions.assertEquals(1, reportChrome.getUnknownCount());
    }

    @Test
    public void testTagCombinationCriteriaMatching()
    {
        // Link bug with severity BLOCKER bound to "browser: Safari, os: iOS, mobile"
        dataService.addBugToExecution(
            RUN_MOBILE_IOS, "row-mobile-1", "BUG-IOS-MOBILE", "BLOCKER", "browser: Safari, os: iOS, mobile"
        );

        // 1. Mobile iOS run matches all tags -> should receive bug and flip to failed-known
        final RunReportDto reportMobile = dataService.getRunReport(RUN_MOBILE_IOS);
        final TestExecutionDto mobileExec = reportMobile.getExecutions().get(0);
        Assertions.assertEquals(List.of("BUG-IOS-MOBILE"), mobileExec.getBugs());
        Assertions.assertEquals("failed-known", mobileExec.getStatus());
        Assertions.assertEquals("BLOCKER", mobileExec.getBugSeverity());

        // 2. Desktop Safari run has browser: Safari and os: macOS, but lacks os: iOS and mobile -> should NOT receive bug
        final RunReportDto reportDesktop = dataService.getRunReport(RUN_DESKTOP);
        final TestExecutionDto desktopExec = reportDesktop.getExecutions().get(0);
        Assertions.assertTrue(desktopExec.getBugs().isEmpty(), "Desktop Safari run should NOT receive bug bound to mobile iOS");
        Assertions.assertEquals("failed-unknown", desktopExec.getStatus());
    }

    @Test
    public void testIngestionMatchesTagCriteria()
    {
        // Link bug with bound tags "browser: Safari, os: iOS"
        dataService.addBugToExecution(
            RUN_MOBILE_IOS, "row-mobile-1", "BUG-INGEST-1", "MAJOR", "browser: Safari, os: iOS"
        );

        final String runIngest = "run-ingest-test";
        final TestRunEntity runEntity = new TestRunEntity(
            runIngest, BATCH, "IN_PROGRESS", "Trigger", "Staging", "US", "Safari", "2026-10-28 10:05:00", System.currentTimeMillis()
        );
        runRepository.save(runEntity);

        // Ingest execution with matching tags
        final Map<String, Object> payload = new HashMap<>();
        payload.put("id", "row-ingest-1");
        payload.put("runId", runIngest);
        payload.put("testClass", "com.xceptance.neodymium.test.LoginTest");
        payload.put("testMethod", "testLogin");
        payload.put("title", "LoginTest testLogin");
        payload.put("status", "failed");
        payload.put("location", "US");
        payload.put("browser", "Safari");
        payload.put("tags", Map.of("browser", List.of("Safari"), "os", List.of("iOS")));

        dataService.ingestExecution(runIngest, payload);

        final RunReportDto report = dataService.getRunReport(runIngest);
        final TestExecutionDto ingestedExec = report.getExecutions().get(0);
        Assertions.assertEquals(List.of("BUG-INGEST-1"), ingestedExec.getBugs());
        Assertions.assertEquals("failed-known", ingestedExec.getStatus());
    }

    private void createRunAndReportWithTags(
        final String runId,
        final String batchName,
        final String environment,
        final String location,
        final long startTime,
        final String rowId,
        final String status,
        final String browser,
        final Map<String, List<String>> tags) throws IOException
    {
        final TestRunEntity runEntity = new TestRunEntity(
            runId, batchName, "COMPLETED", "Trigger", environment, location, browser, "2026-10-28 10:00:00", startTime
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runRepository.save(runEntity);

        final TestExecutionDto exec = new TestExecutionDto(
            rowId, runId, "com.xceptance.neodymium.test.PaymentTest", "submitPayment",
            "PaymentTest submitPayment", status, location, browser, tags
        );
        exec.setEnvironment(environment);
        exec.setBatchName(batchName);

        final RunReportDto report = new RunReportDto(
            runId, batchName, environment, "2026-10-28 10:00:00", "1.0s", 1,
            0, 0, 0, 1, 0, 0, List.of(exec), List.of(), 0, 0L, 0.0,
            Map.of("environment", environment, "batch", batchName)
        );

        dataService.saveRunReportToDisk(runId, report);
    }

    @Test
    public void testLocationAndEnvironmentCriteriaBugBinding() throws IOException
    {
        final String runStgDe = "run-stg-de";
        final String runStgUs = "run-stg-us";
        final String runProdDe = "run-prod-de";

        createRunAndReportWithTags(
            runStgDe, BATCH, "STG", "DE", System.currentTimeMillis() - 40000L,
            "row-stg-de", "failed", "Chrome", Map.of("Area", List.of("Checkout"))
        );
        createRunAndReportWithTags(
            runStgUs, BATCH, "STG", "US", System.currentTimeMillis() - 30000L,
            "row-stg-us", "failed", "Chrome", Map.of("Area", List.of("Checkout"))
        );
        createRunAndReportWithTags(
            runProdDe, BATCH, "PROD", "DE", System.currentTimeMillis() - 20000L,
            "row-prod-de", "failed", "Chrome", Map.of("Area", List.of("Checkout"))
        );

        // Link bug to runStgDe with "location: DE, environment: STG"
        final TestExecutionDto result = dataService.addBugToExecution(
            runStgDe, "row-stg-de", "BUG-6447", "MAJOR", "location: DE, environment: STG"
        );

        // 1. Target run (STG + DE) must match and have the bug
        Assertions.assertEquals(List.of("BUG-6447"), result.getBugs());
        Assertions.assertEquals("failed-known", result.getStatus());
        Assertions.assertEquals("MAJOR", result.getBugSeverity());

        // 2. Fetch report for runStgDe -> must show bug linked
        final RunReportDto reportStgDe = dataService.getRunReport(runStgDe);
        Assertions.assertEquals(List.of("BUG-6447"), reportStgDe.getExecutions().get(0).getBugs());
        Assertions.assertEquals("failed-known", reportStgDe.getExecutions().get(0).getStatus());

        // 3. STG + US run -> must NOT match because location != DE
        final RunReportDto reportStgUs = dataService.getRunReport(runStgUs);
        Assertions.assertTrue(reportStgUs.getExecutions().get(0).getBugs().isEmpty());
        Assertions.assertEquals("failed-unknown", reportStgUs.getExecutions().get(0).getStatus());

        // 4. PROD + DE run -> must NOT match because environment != STG
        final RunReportDto reportProdDe = dataService.getRunReport(runProdDe);
        Assertions.assertTrue(reportProdDe.getExecutions().get(0).getBugs().isEmpty());
        Assertions.assertEquals("failed-unknown", reportProdDe.getExecutions().get(0).getStatus());

        // 5. Test getAllKnownTagsList contains expected tags
        final List<String> knownTags = dataService.getAllKnownTagsList();
        Assertions.assertTrue(knownTags.contains("environment: STG"));
        Assertions.assertTrue(knownTags.contains("environment: PROD"));
        Assertions.assertTrue(knownTags.contains("batch: TagBugBatch"));

        // 6. Test getAllKnownTagMap structure
        final Map<String, List<String>> tagMap = dataService.getAllKnownTagMap();
        Assertions.assertTrue(tagMap.containsKey("environment"));
        Assertions.assertTrue(tagMap.get("environment").contains("STG"));
        Assertions.assertTrue(tagMap.get("environment").contains("PROD"));
        Assertions.assertTrue(tagMap.containsKey("batch"));
        Assertions.assertTrue(tagMap.get("batch").contains("TagBugBatch"));
        Assertions.assertTrue(tagMap.containsKey("browser"));
        Assertions.assertTrue(tagMap.get("browser").contains("Chrome"));
        Assertions.assertTrue(tagMap.containsKey("location"));
        Assertions.assertTrue(tagMap.get("location").contains("DE"));
        Assertions.assertTrue(tagMap.get("location").contains("US"));
    }

    @Test
    public void testGetAllKnownTagMapDiscoversAllDimensions()
    {
        // 1. Add run with DEV environment and FR locale
        final TestRunEntity devRun = new TestRunEntity(
            "run-dev-fr", "DevSuite", "COMPLETED", "Manual", "DEV", "FR,DE", "Chrome,Firefox", "10:00", System.currentTimeMillis()
        );
        runRepository.save(devRun);

        // 2. Add batch with SANDBOX environment
        batchRepository.save(new TestBatchEntity("SandboxBatch", "SANDBOX", "Sandbox desc", "ES", "Edge", "run-dev-fr"));

        // 3. Add variation with UAT environment and custom area
        final TestBaseVariationEntity varUat = new TestBaseVariationEntity(
            "var-uat-1", "com.xceptance.CheckoutTest", "Dataset1", "@Payment", "UK", "Safari"
        );
        varUat.setLastEnvironment("UAT");
        varUat.setLastBatch("UatBatch");
        variationRepository.save(varUat);

        // 4. Add custom tags in tagRepository
        tagRepository.save(new TestRunTagEntity("run-dev-fr", "Branch", "develop"));
        tagRepository.save(new TestRunTagEntity("run-dev-fr", "Release", "2026.11"));

        // 5. Query getAllKnownTagMap
        final Map<String, List<String>> tagMap = dataService.getAllKnownTagMap();

        // Verify standard dimensions are present
        Assertions.assertTrue(tagMap.containsKey("environment"), "environment key must exist");
        Assertions.assertTrue(tagMap.get("environment").contains("DEV"), "DEV must be in environment");
        Assertions.assertTrue(tagMap.get("environment").contains("SANDBOX"), "SANDBOX must be in environment");
        Assertions.assertTrue(tagMap.get("environment").contains("UAT"), "UAT must be in environment");
        Assertions.assertTrue(tagMap.get("environment").contains("Staging"), "Staging from setUp must be in environment");

        Assertions.assertTrue(tagMap.containsKey("batch"), "batch key must exist");
        Assertions.assertTrue(tagMap.get("batch").contains("DevSuite"), "DevSuite must be in batch");
        Assertions.assertTrue(tagMap.get("batch").contains("SandboxBatch"), "SandboxBatch must be in batch");
        Assertions.assertTrue(tagMap.get("batch").contains("UatBatch"), "UatBatch must be in batch");

        Assertions.assertTrue(tagMap.containsKey("browser"), "browser key must exist");
        Assertions.assertTrue(tagMap.get("browser").contains("Chrome"), "Chrome must be in browser");
        Assertions.assertTrue(tagMap.get("browser").contains("Firefox"), "Firefox must be in browser");
        Assertions.assertTrue(tagMap.get("browser").contains("Edge"), "Edge must be in browser");
        Assertions.assertTrue(tagMap.get("browser").contains("Safari"), "Safari must be in browser");

        Assertions.assertTrue(tagMap.containsKey("location"), "location key must exist");
        Assertions.assertTrue(tagMap.get("location").contains("FR"), "FR must be in location");
        Assertions.assertTrue(tagMap.get("location").contains("DE"), "DE must be in location");
        Assertions.assertTrue(tagMap.get("location").contains("ES"), "ES must be in location");
        Assertions.assertTrue(tagMap.get("location").contains("UK"), "UK must be in location");

        Assertions.assertTrue(tagMap.containsKey("area"), "area key must exist");
        Assertions.assertTrue(tagMap.get("area").contains("Payment"), "Payment area must be in area");

        // Verify custom dimensions are canonicalized to lowercase
        Assertions.assertTrue(tagMap.containsKey("branch"), "branch custom key must exist");
        Assertions.assertTrue(tagMap.get("branch").contains("develop"), "develop must be under branch");

        Assertions.assertTrue(tagMap.containsKey("release"), "release custom key must exist");
        Assertions.assertTrue(tagMap.get("release").contains("2026.11"), "2026.11 must be under release");

        // Verify flattened list as well
        final List<String> tagList = dataService.getAllKnownTagsList();
        Assertions.assertTrue(tagList.contains("environment: DEV"));
        Assertions.assertTrue(tagList.contains("environment: SANDBOX"));
        Assertions.assertTrue(tagList.contains("environment: UAT"));
        Assertions.assertTrue(tagList.contains("location: FR"));
        Assertions.assertTrue(tagList.contains("branch: develop"));

        // Verify getCandidateCustomTags excludes standard dimensions and retains custom keys
        final Map<String, List<String>> candidateCustomTags = dataService.getCandidateCustomTags();
        Assertions.assertNotNull(candidateCustomTags);
        Assertions.assertFalse(candidateCustomTags.containsKey("browser"), "Standard browser dimension must be excluded");
        Assertions.assertFalse(candidateCustomTags.containsKey("environment"), "Standard environment dimension must be excluded");
        Assertions.assertFalse(candidateCustomTags.containsKey("location"), "Standard location dimension must be excluded");
        Assertions.assertFalse(candidateCustomTags.containsKey("area"), "Standard area dimension must be excluded");
        Assertions.assertFalse(candidateCustomTags.containsKey("batch"), "Standard batch dimension must be excluded");
        Assertions.assertFalse(candidateCustomTags.containsKey("suite"), "Standard suite dimension must be excluded");
        Assertions.assertFalse(candidateCustomTags.containsKey("batchname"), "Standard batchname dimension must be excluded");
        Assertions.assertTrue(candidateCustomTags.containsKey("Branch"), "Branch custom tag must be present with preserved casing");
        Assertions.assertTrue(candidateCustomTags.get("Branch").contains("develop"), "Branch must contain develop");
        Assertions.assertTrue(candidateCustomTags.containsKey("Release"), "Release custom tag must be present with preserved casing");
        Assertions.assertTrue(candidateCustomTags.get("Release").contains("2026.11"), "Release must contain 2026.11");

        // Verify run-level tag matching on execution
        final TestExecutionDto sampleExec = new TestExecutionDto("sample-1", "run-dev-fr", "CheckoutTest", "testCheckout", "Checkout", "passed", "FR", "Chrome", Map.of());
        final Map<String, List<String>> runTags = dataService.getRunTagsMap("run-dev-fr");
        Assertions.assertTrue(AuraReportDataService.matchesBugTags(sampleExec, "DEV", "DevSuite", runTags, "branch: develop"), "Bug bound to branch: develop must match run with branch: develop");
        Assertions.assertTrue(AuraReportDataService.matchesBugTags(sampleExec, "DEV", "DevSuite", runTags, "release: 2026.11"), "Bug bound to release: 2026.11 must match run with release: 2026.11");
        Assertions.assertFalse(AuraReportDataService.matchesBugTags(sampleExec, "DEV", "DevSuite", runTags, "branch: main"), "Bug bound to branch: main must NOT match run with branch: develop");
    }

    @Test
    public void testCrossEnvironmentBugLinkageAndInactiveBugDiscovery() throws IOException
    {
        // 1. Link bug on Staging run (RUN_SAFARI) bound to "environment: DEV"
        final TestExecutionDto savedResult = dataService.addBugToExecution(
            RUN_SAFARI, "row-safari-1", "BUG-DEV-ONLY", "CRITICAL", "environment: DEV"
        );

        // 2. Since this run is Staging, the bug does NOT apply actively to row-safari-1
        Assertions.assertTrue(savedResult.getBugs().isEmpty(), "Active bugs list must be empty on non-matching environment");
        Assertions.assertNotEquals("failed-known", savedResult.getStatus(), "Status must not change to failed-known on non-matching environment");

        // 3. But it MUST be discoverable in inactiveBugDetails for clear UI feedback
        Assertions.assertNotNull(savedResult.getInactiveBugDetails(), "Inactive bug details must not be null");
        Assertions.assertEquals(1, savedResult.getInactiveBugDetails().size(), "Must have 1 inactive bug detail");
        final BugDetailDto inactiveDetail = savedResult.getInactiveBugDetails().get(0);
        Assertions.assertEquals("BUG-DEV-ONLY", inactiveDetail.getBugTicket());
        Assertions.assertEquals("CRITICAL", inactiveDetail.getSeverity());
        Assertions.assertEquals("environment: DEV", inactiveDetail.getTagCriteria());

        // 4. Verify database persistence scope: must be ALL / ALL so DEV runs can query it
        final List<TestBaseBugEntity> allDbBugs = bugRepository.findAll();
        final TestBaseBugEntity entity = allDbBugs.stream()
            .filter(b -> "BUG-DEV-ONLY".equalsIgnoreCase(b.getBugTicket()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(entity, "Bug must be persisted in database");
        Assertions.assertEquals("ALL", entity.getBatchName(), "Batch name must be ALL for tag-bound bug");
        Assertions.assertEquals("ALL", entity.getEnvironment(), "Environment must be ALL for tag-bound bug");
        Assertions.assertEquals("environment: DEV", entity.getTagCriteria());

        // 5. Query Staging report: must have inactive bug detail, no active bug
        final RunReportDto stgReport = dataService.getRunReport(RUN_SAFARI);
        final TestExecutionDto stgExec = stgReport.getExecutions().get(0);
        Assertions.assertTrue(stgExec.getBugs().isEmpty(), "Staging execution must have no active bugs");
        Assertions.assertEquals(1, stgExec.getInactiveBugDetails().size(), "Staging execution must have inactive bug detail");

        // 6. Create a DEV run for the same test method and variation
        final String runDev = "run-dev-1";
        final long now = System.currentTimeMillis();
        final TestRunEntity devRunEntity = new TestRunEntity(
            runDev, "DevBatch", "COMPLETED", "Trigger", "DEV", "US", "Safari", "2026-10-28 11:00:00", now
        );
        devRunEntity.setTotalTests(1);
        devRunEntity.setFailedUnknownCount(1);
        runRepository.save(devRunEntity);

        final TestExecutionDto devExec = new TestExecutionDto(
            "row-dev-1", runDev, "com.xceptance.neodymium.test.LoginTest", "testLogin",
            "LoginTest testLogin", "failed", "Safari", Map.of()
        );
        final RunReportDto devReportDto = new RunReportDto(
            runDev, "DevBatch", "2026-10-28 11:00:00", "1.0s", 1,
            0, 0, 0, 1, 0, List.of(devExec)
        );
        dataService.saveRunReportToDisk(runDev, devReportDto);

        // 7. Query DEV report: the bug MUST match dynamically and become active!
        final RunReportDto loadedDevReport = dataService.getRunReport(runDev);
        final TestExecutionDto loadedDevExec = loadedDevReport.getExecutions().get(0);
        Assertions.assertEquals(List.of("BUG-DEV-ONLY"), loadedDevExec.getBugs(), "DEV execution must actively have BUG-DEV-ONLY");
        Assertions.assertEquals("failed-known", loadedDevExec.getStatus(), "DEV execution must be marked as failed-known");
        Assertions.assertTrue(loadedDevExec.getInactiveBugDetails().isEmpty(), "DEV execution must have 0 inactive bugs");
    }
}
