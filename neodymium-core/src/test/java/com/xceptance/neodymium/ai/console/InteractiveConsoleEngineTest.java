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
package com.xceptance.neodymium.ai.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.report.TestExecutionReport.CategoryTokenUsage;
import org.neodymium.ai.report.TestExecutionReport.ReportActionEntry;
import org.neodymium.ai.report.TestExecutionReport.ReportMetrics;
import org.neodymium.ai.report.TestExecutionReport.ReportStepEntry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.ActionResult;

/**
 * Unit test suite validating {@link InteractiveConsoleEngine} console execution logging.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class InteractiveConsoleEngineTest
{
    @TempDir
    public File tempResultsDir;

    private String originalResultsDir;
    private String originalConsoleLogsSetting;

    @BeforeEach
    public void setUp()
    {
        originalResultsDir = System.getProperty("allure.results.directory");
        originalConsoleLogsSetting = System.getProperty("neodymium.ai.consoleExecutionLogs");
        System.setProperty("allure.results.directory", tempResultsDir.getAbsolutePath());
        InteractiveConsoleEngine.resetExecutionIndexes();
        AiConfiguration.resetInstance();
    }

    @AfterEach
    public void tearDown()
    {
        InteractiveConsoleEngine.resetExecutionIndexes();
        if (originalResultsDir != null)
        {
            System.setProperty("allure.results.directory", originalResultsDir);
        }
        else
        {
            System.clearProperty("allure.results.directory");
        }

        if (originalConsoleLogsSetting != null)
        {
            System.setProperty("neodymium.ai.consoleExecutionLogs", originalConsoleLogsSetting);
        }
        else
        {
            System.clearProperty("neodymium.ai.consoleExecutionLogs");
        }
        System.clearProperty("neodymium.ai.consoleExecutionLogs.directory");
        AiConfiguration.resetInstance();
    }

    @Test
    public void testPushStateWritesFileWhenConsoleExecutionLogsEnabled() throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-unit");
        final String jsonPayload = "{\"testName\":\"UnitTestCase1\",\"status\":\"passed\",\"steps\":[]}";

        engine.pushState(jsonPayload);

        final File expectedLogFile = new File(tempResultsDir, "console-execution-1.json");
        assertTrue(expectedLogFile.exists(), "console-execution-1.json should be created");

        final String content = Files.readString(expectedLogFile.toPath());
        assertTrue(content.contains("UnitTestCase1"), "Log content should contain test name");
        assertTrue(content.contains("\"status\":\"passed\""), "Log content should contain status");
    }

    @Test
    public void testPushStateDoesNotWriteFileWhenConsoleExecutionLogsDisabled()
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "false");
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-unit");
        final String jsonPayload = "{\"testName\":\"DisabledTestCase\",\"status\":\"passed\"}";

        engine.pushState(jsonPayload);

        final File expectedLogFile = new File(tempResultsDir, "console-execution-1.json");
        assertFalse(expectedLogFile.exists(), "console-execution-1.json should NOT be created when logging is disabled");
    }

    @Test
    public void testPushStateWritesFileToCustomConfiguredDirectory(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path customDir) throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", customDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-custom");
        final String jsonPayload = "{\"testName\":\"CustomDirTestCase\",\"status\":\"passed\",\"steps\":[]}";

        engine.pushState(jsonPayload);

        final String runFolder = InteractiveConsoleEngine.getRunFolder();
        final File expectedLogFile = new File(customDir.toFile(), runFolder + "/CustomDirTestCase/console-execution-1.json");
        assertTrue(expectedLogFile.exists(), "console-execution-1.json should be created in custom configured run_ directory");
    }

    @Test
    public void testPushStateExtractsExecutionKeyAndIncrementsIndexes() throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-unit");

        engine.pushState("{\"testName\":\"AlphaTest · ds1\",\"status\":\"passed\"}");
        engine.pushState("{\"testName\":\"AlphaTest · ds2\",\"status\":\"passed\"}");

        final File log1 = new File(tempResultsDir, "console-execution-1.json");
        final File log2 = new File(tempResultsDir, "console-execution-2.json");

        assertTrue(log1.exists(), "console-execution-1.json should exist for ds1");
        assertTrue(log2.exists(), "console-execution-2.json should exist for ds2");

        final String content1 = Files.readString(log1.toPath());
        final String content2 = Files.readString(log2.toPath());

        assertTrue(content1.contains("ds1"), "log1 should contain ds1");
        assertTrue(content2.contains("ds2"), "log2 should contain ds2");
    }

    @Test
    public void testPushStateAcrossMultipleEngineInstances() throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engineDataset1 = new InteractiveConsoleEngine("run-ds1");
        engineDataset1.pushState("{\"testName\":\"WikipediaSearchTest · dataset_1\",\"status\":\"passed\"}");

        final InteractiveConsoleEngine engineDataset2 = new InteractiveConsoleEngine("run-ds2");
        engineDataset2.pushState("{\"testName\":\"WikipediaSearchTest · dataset_2\",\"status\":\"failed\"}");

        final File log1 = new File(tempResultsDir, "console-execution-1.json");
        final File log2 = new File(tempResultsDir, "console-execution-2.json");

        assertTrue(log1.exists(), "console-execution-1.json should exist for Dataset 1");
        assertTrue(log2.exists(), "console-execution-2.json should exist for Dataset 2");

        final String content1 = Files.readString(log1.toPath());
        final String content2 = Files.readString(log2.toPath());

        assertTrue(content1.contains("dataset_1"), "log1 should contain dataset_1");
        assertTrue(content1.contains("\"status\":\"passed\""), "log1 status should be passed");

        assertTrue(content2.contains("dataset_2"), "log2 should contain dataset_2");
        assertTrue(content2.contains("\"status\":\"failed\""), "log2 status should be failed");
    }

    @Test
    public void testBuildStateJsonSerializesFailureReason()
    {
        final org.neodymium.ai.pipeline.ExecutionContext context = new org.neodymium.ai.pipeline.ExecutionContext(null);

        final org.neodymium.ai.model.PlaybookStep step1 = new org.neodymium.ai.model.PlaybookStep();
        step1.setInstruction("Step 1");
        step1.setStatus(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS);

        final org.neodymium.ai.model.PlaybookStep step2 = new org.neodymium.ai.model.PlaybookStep();
        step2.setInstruction("Step 2");
        step2.setStatus(org.neodymium.ai.model.PlaybookStepStatus.FAILED);
        step2.setFailureReason("Element not found exception: #missingElement");

        final java.util.List<org.neodymium.ai.model.PlaybookStep> flatSteps = java.util.List.of(step1, step2);
        context.getTransientData().put("playbook.flatSteps", flatSteps);

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-err", 1, "failed");

        assertTrue(stateJson.contains("\"error\":\"Element not found exception: #missingElement\""), "Top-level state JSON should contain error message");
        assertTrue(stateJson.contains("\"failureReason\":\"Element not found exception: #missingElement\""), "State JSON step/top-level should contain failureReason");
    }

    @Test
    public void testBuildStateJsonSerializesBrowser()
    {
        final org.neodymium.ai.pipeline.ExecutionContext context = new org.neodymium.ai.pipeline.ExecutionContext(null);
        context.getTransientData().put("browser", "Chrome_1024x768");

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-browser", 0, "running");

        assertTrue(stateJson.contains("\"browser\":\"Chrome_1024x768\""), "Top-level state JSON should contain browser property");
    }

    @Test
    public void testBuildStateJsonSerializesExecutionModeAndStepOrigin()
    {
        final org.neodymium.ai.pipeline.ExecutionContext context = new org.neodymium.ai.pipeline.ExecutionContext(null);
        context.getTransientData().put(org.neodymium.ai.pipeline.ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.REPLAY_WITH_HEALING);

        final org.neodymium.ai.model.PlaybookStep step1 = new org.neodymium.ai.model.PlaybookStep();
        step1.setInstruction("Replayed step");
        step1.setSourceFile("playbook.json");
        step1.setStatus(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS);

        final org.neodymium.ai.model.PlaybookStep step2 = new org.neodymium.ai.model.PlaybookStep();
        step2.setInstruction("Healed step");
        step2.setSourceFile("playbook.json");
        step2.setStatus(org.neodymium.ai.model.PlaybookStepStatus.HEALED);

        context.getTransientData().put("playbook.flatSteps", java.util.List.of(step1, step2));

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-origin", 0, "running");

        assertTrue(stateJson.contains("\"executionMode\":\"REPLAY_WITH_HEALING\""), "Top-level state JSON should contain executionMode");
        assertTrue(stateJson.contains("\"origin\":\"PLAYBOOK\""), "Replayed step origin should be PLAYBOOK");
        assertTrue(stateJson.contains("\"origin\":\"HEALED\""), "Healed step origin should be HEALED");
    }

    @Test
    public void testBuildStateJsonSerializesStartTimesAndStepDuration()
    {
        final org.neodymium.ai.pipeline.ExecutionContext context = new org.neodymium.ai.pipeline.ExecutionContext(null);

        final org.neodymium.ai.model.PlaybookStep step = new org.neodymium.ai.model.PlaybookStep();
        step.setInstruction("Timed step");
        step.setStartTimeMs(1787140134811L);
        step.setDurationMs(1250L);
        step.setStatus(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS);

        context.getTransientData().put("playbook.flatSteps", java.util.List.of(step));

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-timestamps", 0, "running");

        assertTrue(stateJson.contains("\"startTime\":"), "Top-level state JSON should contain startTime");
        assertTrue(stateJson.contains("\"startTimestamp\":"), "Step JSON should contain startTimestamp");
        assertTrue(stateJson.contains("\"duration\":1250"), "Step JSON should contain duration");
    }

    @Test
    public void testBuildStateJsonSerializesSsimFields()
    {
        final org.neodymium.ai.model.SessionData sessionData = new org.neodymium.ai.model.SessionData();
        final org.neodymium.ai.pipeline.ExecutionContext context = new org.neodymium.ai.pipeline.ExecutionContext(sessionData);
        ExecutionContext.setActiveContext(context);

        try
        {
            final org.neodymium.ai.model.PlaybookStep step = new org.neodymium.ai.model.PlaybookStep();
            step.setInstruction("Verify neodymium metal picture is displayed");
            step.setStatus(org.neodymium.ai.model.PlaybookStepStatus.FAILED);
            step.setSsimScore(0.4708);
            step.setSsimMinScore(0.99);
            step.setBaselineMatrixPng("data:image/png;base64,recordedBaselineData");
            step.setReplayMatrixPng("data:image/png;base64,replayCurrentData");
            step.setScreenshotHashDim(128);

            context.getTransientData().put("playbook.flatSteps", java.util.List.of(step));

            final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-ssim", 0, "failed");

            assertTrue(stateJson.contains("\"ssimScore\":0.4708"), "State JSON should contain ssimScore from PlaybookStep");
            assertTrue(stateJson.contains("\"ssimMinScore\":0.99"), "State JSON should contain ssimMinScore from PlaybookStep");
            assertTrue(stateJson.contains("recordedBaselineData"), "State JSON should contain baselineMatrixPng from PlaybookStep");
            assertTrue(stateJson.contains("replayCurrentData"), "State JSON should contain replayMatrixPng from PlaybookStep");
            assertTrue(stateJson.contains("\"screenshotHashDim\":128"), "State JSON should contain screenshotHashDim from PlaybookStep");
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    public void testExtractLocaleFromDatasetId()
    {
        assertEquals("DE", InteractiveStateBuilder.extractLocaleFromDatasetId("homepage test DE"));
        assertEquals("deu", InteractiveStateBuilder.extractLocaleFromDatasetId("guest checkout deu"));
        assertEquals("en_US", InteractiveStateBuilder.extractLocaleFromDatasetId("checkout_en_US"));
        assertEquals("FR", InteractiveStateBuilder.extractLocaleFromDatasetId("search_test_FR"));
        assertEquals("uk", InteractiveStateBuilder.extractLocaleFromDatasetId("test.uk"));
        assertNull(InteractiveStateBuilder.extractLocaleFromDatasetId("wikipedia_search_example_1"));
    }

    @Test
    public void testBuildStateJsonSerializesLocale()
    {
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put("datasetLabel", "homepage test DE");

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-locale", 0, "running");

        assertTrue(stateJson.contains("\"locale\":\"DE\""), "Top-level state JSON should contain guessed locale 'DE'");
    }

    @Test
    public void testBuildStateJsonSerializesLinterCategories()
    {
        final ExecutionContext context = new ExecutionContext(null);
        final TestExecutionReport report = new TestExecutionReport();
        final ReportMetrics metrics = report.getMetrics();
        metrics.setLinter(new CategoryTokenUsage(1, 100, 50, 0, 0.001));
        metrics.setPostFlightLinter(new CategoryTokenUsage(2, 200, 80, 0, 0.002));
        context.getTransientData().put("testExecutionReport", report);

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "test-run-linter-cats", 0, "running");

        assertTrue(stateJson.contains("\"linter\":{"), "State JSON should contain linter category");
        assertTrue(stateJson.contains("\"postFlightLinter\":{"), "State JSON should contain postFlightLinter category");
    }

    @Test
    public void testPushStatePerformsPerClassExecutionIndexing(@TempDir final Path customDir) throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", customDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-per-class");
        
        // Push state for Class A
        engine.pushState("{\"testName\":\"AlphaTestClass · ds1\",\"testFile\":\"com.xceptance.AlphaTestClass#execute\",\"status\":\"passed\",\"steps\":[]}");
        // Push state for Class B
        engine.pushState("{\"testName\":\"BetaTestClass · ds1\",\"testFile\":\"com.xceptance.BetaTestClass#execute\",\"status\":\"passed\",\"steps\":[]}");

        final String runFolder = InteractiveConsoleEngine.getRunFolder();
        final File alphaLog = new File(customDir.toFile(), runFolder + "/AlphaTestClass/console-execution-1.json");
        final File betaLog = new File(customDir.toFile(), runFolder + "/BetaTestClass/console-execution-1.json");

        assertTrue(alphaLog.exists(), "AlphaTestClass should start execution indexing at 1 (console-execution-1.json)");
        assertTrue(betaLog.exists(), "BetaTestClass should independently start execution indexing at 1 (console-execution-1.json)");
    }

    @Test
    public void testAttachVideoToLatestExecutionLog(@TempDir final Path customDir) throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", customDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-attach-video");

        engine.pushState("{\"testName\":\"VideoTestClass · ds1\",\"testFile\":\"com.xceptance.VideoTestClass#execute\",\"status\":\"passed\",\"steps\":[]}");

        final File dummyVideo = new File(tempResultsDir, "recording-dummy.mp4");
        Files.writeString(dummyVideo.toPath(), "dummy-mp4-data");

        InteractiveConsoleEngine.attachVideoToLatestExecutionLog(dummyVideo, "VideoTestClass");

        final String runFolder = InteractiveConsoleEngine.getRunFolder();
        final File targetVideo = new File(customDir.toFile(), runFolder + "/VideoTestClass/video-1.mp4");
        final File updatedJson = new File(customDir.toFile(), runFolder + "/VideoTestClass/console-execution-1.json");

        assertTrue(targetVideo.exists(), "Video file should be copied to run storage folder as video-1.mp4");
        assertTrue(updatedJson.exists(), "console-execution-1.json should exist");
        final String jsonContent = Files.readString(updatedJson.toPath());
        assertTrue(jsonContent.contains("\"videoUrl\":\"video-1.mp4\""), "JSON should contain videoUrl attribute");
    }

    @Test
    public void testAttachVideoToCustomNamedExecutionLog(@TempDir final Path customDir) throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", customDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        final String runFolder = InteractiveConsoleEngine.getRunFolder();
        final File targetTestDir = new File(customDir.toFile(), runFolder + "/Aura_my_test_yaml_Test");
        targetTestDir.mkdirs();

        final File customJson = new File(targetTestDir, "Aura_my_test_yaml_Test#executeYamlTest#en#Chrome_1920x1080.json");
        Files.writeString(customJson.toPath(), "{\"testName\":\"Aura_my_test_yaml_Test :: executeYamlTest\",\"status\":\"failed\"}");

        final File dummyVideo = new File(tempResultsDir, "custom-recording-dummy.mp4");
        Files.writeString(dummyVideo.toPath(), "custom-mp4-data");

        InteractiveConsoleEngine.attachVideoToLatestExecutionLog(dummyVideo, "Aura_my_test_yaml_Test :: executeYamlTest");

        final File targetVideo = new File(targetTestDir, "video-1.mp4");
        assertTrue(targetVideo.exists(), "Video file should be copied to run storage folder as video-1.mp4");
        assertTrue(customJson.exists(), "Custom execution log JSON should exist");

        final String updatedJsonContent = Files.readString(customJson.toPath());
        assertTrue(updatedJsonContent.contains("\"videoUrl\":\"video-1.mp4\""), "Custom execution JSON should contain videoUrl attribute");
        assertTrue(updatedJsonContent.contains("\"videoPath\":\"video-1.mp4\""), "Custom execution JSON should contain videoPath attribute");
    }

    @Test
    public void testStateJsonSerializesHealedActionTargetAndFlag() throws Exception
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PlaybookStep pbStep = new PlaybookStep("Select language");
        pbStep.setLineNumber(1);
        context.getTransientData().put("playbook.flatSteps", List.of(pbStep));

        final TestExecutionReport report = new TestExecutionReport();
        final ReportStepEntry step = new ReportStepEntry();
        step.setStepIndex(0);
        step.setRawInstruction("Select language");

        final ReportActionEntry action = new ReportActionEntry(
            "SELECT",
            "#selectLanguage",
            "en",
            "Select 'en' on #selectLanguage",
            "",
            true,
            null,
            true
        );
        action.setResolvedTarget("#searchLanguage");
        action.setResolvedValue("en");
        step.addAction(action);
        report.addStep(step);

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run_test_healed", 0, "passed", null, report);
        final JsonNode rootNode = new ObjectMapper().readTree(stateJson);
        final JsonNode actionNode = rootNode.path("blocks").path("steps").get(0).path("actions").get(0);

        assertEquals("#searchLanguage", actionNode.path("target").asText());
        assertEquals("#searchLanguage", actionNode.path("resolvedTarget").asText());
        assertEquals(true, actionNode.path("healed").asBoolean());
        assertEquals("#selectLanguage", actionNode.path("expectedTarget").asText());
        assertEquals("#selectLanguage", actionNode.path("originalTarget").asText());
        assertEquals("#selectLanguage", actionNode.path("healedFrom").asText());
    }

    @Test
    public void testStateJsonIncludesLinterFindingsWhenEnabled() throws Exception
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PlaybookLinterFinding finding = new PlaybookLinterFinding(
            1,
            14,
            "checkout.yaml",
            "Open dropdown and click \"${country}\"",
            "Open dropdown and click \"Germany\"",
            LinterCategory.STEP_SPLITTING_CANDIDATE,
            LinterSeverity.WARNING,
            "Instruction combines two interactive actions.",
            "1. Open dropdown\n2. Click \"${country}\"",
            null
        );
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS, List.of(finding));

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run_test_linter", 0, "passed");
        final JsonNode rootNode = new ObjectMapper().readTree(stateJson);

        assertTrue(rootNode.has("linterFindings"), "State JSON should contain linterFindings when linter is enabled");
        final JsonNode findingsArr = rootNode.get("linterFindings");
        assertEquals(1, findingsArr.size());
        final JsonNode f = findingsArr.get(0);
        assertEquals(1, f.path("stepIndex").asInt());
        assertEquals(14, f.path("lineNumber").asInt());
        assertEquals("checkout.yaml", f.path("sourceFile").asText());
        assertEquals("Open dropdown and click \"${country}\"", f.path("rawInstruction").asText());
        assertEquals("Open dropdown and click \"Germany\"", f.path("resolvedInstruction").asText());
        assertEquals("STEP_SPLITTING_CANDIDATE", f.path("category").asText());
        assertEquals("WARNING", f.path("severity").asText());
        assertEquals("Instruction combines two interactive actions.", f.path("message").asText());
        assertEquals("1. Open dropdown\n2. Click \"${country}\"", f.path("suggestedRewrite").asText());
    }

    @Test
    public void testStateJsonOmitsLinterFindingsWhenDisabled() throws Exception
    {
        final String orig = System.getProperty("neodymium.ai.linter.enabled");
        try
        {
            System.setProperty("neodymium.ai.linter.enabled", "false");
            AiConfiguration.resetInstance();

            final ExecutionContext context = new ExecutionContext(new SessionData());
            final PlaybookLinterFinding finding = new PlaybookLinterFinding(
                1,
                "click button",
                "click button",
                LinterCategory.VAGUE_TARGET,
                LinterSeverity.WARNING,
                "Ambiguous target",
                "click #submit",
                null
            );
            context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS, List.of(finding));

            final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run_test_linter_off", 0, "passed");
            final JsonNode rootNode = new ObjectMapper().readTree(stateJson);

            assertFalse(rootNode.has("linterFindings"), "State JSON should omit linterFindings when pre-flight linter is disabled");
        }
        finally
        {
            if (orig != null)
            {
                System.setProperty("neodymium.ai.linter.enabled", orig);
            }
            else
            {
                System.clearProperty("neodymium.ai.linter.enabled");
            }
            AiConfiguration.resetInstance();
        }
    }

    @Test
    public void testStateJsonIncludesPostFlightFindingsWhenEnabled() throws Exception
    {
        final String orig = System.getProperty("neodymium.ai.linter.postFlight.enabled");
        try
        {
            System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
            AiConfiguration.resetInstance();

            final ExecutionContext context = new ExecutionContext(new SessionData());
            final PlaybookLinterFinding finding = new PlaybookLinterFinding(
                2,
                25,
                "cart.yaml",
                "Hover and click",
                "Hover and click",
                LinterCategory.EMPIRICAL_MULTI_ACTION,
                LinterSeverity.INFO,
                "Multiple actions in step",
                "Split actions",
                "ACTION"
            );
            context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS, List.of(finding));

            final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run_test_postflight", 0, "passed");
            final JsonNode rootNode = new ObjectMapper().readTree(stateJson);

            assertTrue(rootNode.has("postFlightFindings"), "State JSON should contain postFlightFindings when post-flight linter is enabled");
            final JsonNode findingsArr = rootNode.get("postFlightFindings");
            assertEquals(1, findingsArr.size());
            final JsonNode f = findingsArr.get(0);
            assertEquals(2, f.path("stepIndex").asInt());
            assertEquals(25, f.path("lineNumber").asInt());
            assertEquals("cart.yaml", f.path("sourceFile").asText());
            assertEquals("EMPIRICAL_MULTI_ACTION", f.path("category").asText());
            assertEquals("INFO", f.path("severity").asText());
            assertEquals("ACTION", f.path("scope").asText());
        }
        finally
        {
            if (orig != null)
            {
                System.setProperty("neodymium.ai.linter.postFlight.enabled", orig);
            }
            else
            {
                System.clearProperty("neodymium.ai.linter.postFlight.enabled");
            }
            AiConfiguration.resetInstance();
        }
    }

    @Test
    public void testStateJsonOmitsPostFlightFindingsWhenDisabled() throws Exception
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PlaybookLinterFinding finding = new PlaybookLinterFinding(
            1,
            "click button",
            "click button",
            LinterCategory.REDUNDANT_VISUAL_TAG,
            LinterSeverity.INFO,
            "Redundant tag",
            "click button",
            null
        );
        context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS, List.of(finding));

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run_test_postflight_off", 0, "passed");
        final JsonNode rootNode = new ObjectMapper().readTree(stateJson);

        assertFalse(rootNode.has("postFlightFindings"), "State JSON should omit postFlightFindings by default when post-flight linter is disabled");
    }

    @Test
    public void testPushStatePersistsLinterAndPostFlightFindingsToDisk() throws Exception
    {
        final String origPost = System.getProperty("neodymium.ai.linter.postFlight.enabled");
        try
        {
            System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
            AiConfiguration.resetInstance();

            final ExecutionContext context = new ExecutionContext(new SessionData());
            context.getTransientData().put("testClass", "com.xceptance.FindingsTestClass");

            final PlaybookLinterFinding preFlight = new PlaybookLinterFinding(
                1,
                "Enter username",
                "Enter admin",
                LinterCategory.STEP_SPLITTING_CANDIDATE,
                LinterSeverity.WARNING,
                "Pre-flight finding",
                "Enter username",
                null
            );
            context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS, List.of(preFlight));

            final PlaybookLinterFinding postFlight = new PlaybookLinterFinding(
                2,
                "Submit form",
                "Submit form",
                LinterCategory.HIGH_AGENT_FRICTION,
                LinterSeverity.WARNING,
                "Post-flight finding",
                "Submit form with wait",
                null
            );
            context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS, List.of(postFlight));

            final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run_test_disk", 0, "passed");
            final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run_test_disk");
            engine.pushState(stateJson);

            final File logFile = new File(tempResultsDir, "console-execution-1.json");
            assertTrue(logFile.exists(), "console-execution-1.json should exist on disk");

            final String loggedJson = Files.readString(logFile.toPath());
            final JsonNode loggedRoot = new ObjectMapper().readTree(loggedJson);

            assertTrue(loggedRoot.has("linterFindings"), "Logged JSON file should have linterFindings");
            assertEquals(1, loggedRoot.get("linterFindings").size());
            assertEquals("Pre-flight finding", loggedRoot.get("linterFindings").get(0).path("message").asText());

            assertTrue(loggedRoot.has("postFlightFindings"), "Logged JSON file should have postFlightFindings");
            assertEquals(1, loggedRoot.get("postFlightFindings").size());
            assertEquals("Post-flight finding", loggedRoot.get("postFlightFindings").get(0).path("message").asText());
        }
        finally
        {
            if (origPost != null)
            {
                System.setProperty("neodymium.ai.linter.postFlight.enabled", origPost);
            }
            else
            {
                System.clearProperty("neodymium.ai.linter.postFlight.enabled");
            }
            AiConfiguration.resetInstance();
        }
    }

    @Test
    public void testArrowSubstepsLoggedAsSubstepsInConsoleExecutionJson() throws Exception
    {
        final String runId = "test-arrow-substeps";
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);

        final ExecutionContext context = new ExecutionContext(new SessionData());

        final PlaybookStep navStep = new PlaybookStep("Navigate to homepage");
        navStep.setLineNumber(1);

        final PlaybookStep arrowParent = new PlaybookStep("Detailed verification of the cart page");
        arrowParent.setLineNumber(3);

        final PlaybookStep sub1 = new PlaybookStep("Verify the cart header is displayed.");
        sub1.setLineNumber(4);
        sub1.setParent(arrowParent);

        final PlaybookStep sub2 = new PlaybookStep("Verify the cart items section is displayed.");
        sub2.setLineNumber(5);
        sub2.setParent(arrowParent);

        arrowParent.setSubSteps(List.of(sub1, sub2));

        final PlaybookStep checkoutStep = new PlaybookStep("Click checkout button");
        checkoutStep.setLineNumber(6);

        context.getTransientData().put("playbook.steps", List.of(navStep, arrowParent, checkoutStep));
        context.getTransientData().put("playbook.flatSteps", List.of(navStep, sub1, sub2, checkoutStep));

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, runId, 1, "running", null, null);
        engine.pushState(stateJson);

        final File logFile = new File(tempResultsDir, "console-execution-1.json");
        assertTrue(logFile.exists(), "console-execution-1.json should exist on disk");

        final String loggedJson = Files.readString(logFile.toPath());
        final JsonNode loggedRoot = new ObjectMapper().readTree(loggedJson);

        final JsonNode stepsNode = loggedRoot.path("blocks").path("steps");
        assertEquals(3, stepsNode.size(), "blocks.steps must contain exactly 3 top-level steps (no independent substeps)");

        assertEquals("Navigate to homepage", stepsNode.get(0).path("instruction").asText());
        assertEquals("Detailed verification of the cart page", stepsNode.get(1).path("instruction").asText());
        assertTrue(stepsNode.get(1).path("hasSubSteps").asBoolean(), "Parent step must declare hasSubSteps: true");

        final JsonNode subStepsArr = stepsNode.get(1).path("subSteps");
        assertEquals(2, subStepsArr.size(), "Parent step must contain 2 substeps");
        assertEquals("Verify the cart header is displayed.", subStepsArr.get(0).path("instruction").asText());
        assertEquals("Verify the cart items section is displayed.", subStepsArr.get(1).path("instruction").asText());

        assertEquals("Click checkout button", stepsNode.get(2).path("instruction").asText());
    }

    @Test
    public void testSkippedCompositeSubstepsDoNotLeakIntoPriorFailedStep() throws Exception
    {
        final String runId = "run_test_failed_composite_leak";
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);

        final ExecutionContext context = new ExecutionContext(new SessionData());

        final PlaybookStep navStep = new PlaybookStep("Navigate to homepage");
        navStep.setLineNumber(1);
        navStep.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep cookieStep = new PlaybookStep("Allow all cookies.");
        cookieStep.setLineNumber(2);
        cookieStep.setStatus(PlaybookStepStatus.FAILED);

        final PlaybookStep addToCartStep = new PlaybookStep("Add to cart");
        addToCartStep.setLineNumber(3);
        final PlaybookStep sub1 = new PlaybookStep("Open search field");
        sub1.setLineNumber(4);
        sub1.setParent(addToCartStep);
        final PlaybookStep sub2 = new PlaybookStep("Type searchTerm");
        sub2.setLineNumber(5);
        sub2.setParent(addToCartStep);
        addToCartStep.setSubSteps(List.of(sub1, sub2));

        context.getTransientData().put("playbook.steps", List.of(navStep, cookieStep, addToCartStep));

        final TestExecutionReport report = new TestExecutionReport();
        final ReportStepEntry rep0 = new ReportStepEntry(0, "Navigate to homepage");
        rep0.setStatus("SUCCESS");
        final ReportStepEntry rep1 = new ReportStepEntry(1, "Allow all cookies.");
        rep1.setStatus("FAILED");
        rep1.setFailureReason("Step timeout of 60s exceeded");
        report.addStep(rep0);
        report.addStep(rep1);

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, runId, 1, "failed", null, report);
        engine.pushState(stateJson);

        final File logFile = new File(tempResultsDir, "console-execution-1.json");
        assertTrue(logFile.exists(), "console-execution-1.json should exist on disk");

        final String loggedJson = Files.readString(logFile.toPath());
        final JsonNode loggedRoot = new ObjectMapper().readTree(loggedJson);

        final JsonNode stepsNode = loggedRoot.path("blocks").path("steps");
        assertEquals(3, stepsNode.size());

        final JsonNode cookieNode = stepsNode.get(1);
        assertEquals("Allow all cookies.", cookieNode.path("instruction").asText());
        assertEquals("failed", cookieNode.path("status").asText());
        assertTrue(!cookieNode.has("subSteps") || cookieNode.path("subSteps").isEmpty(),
            "Failed flat step must NOT have substeps attached");

        final JsonNode cartNode = stepsNode.get(2);
        assertEquals("Add to cart", cartNode.path("instruction").asText());
        assertEquals("skipped", cartNode.path("status").asText());
        assertTrue(cartNode.path("hasSubSteps").asBoolean());
        assertEquals(2, cartNode.path("subSteps").size());
    }

    @Test
    public void testCompositeStepActionsAttributedToSubstepsInConsoleExecutionJson() throws Exception
    {
        final String runId = "run_test_composite_action_attribution";
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine(runId);

        final ExecutionContext context = new ExecutionContext(new SessionData());

        final PlaybookStep parentStep = new PlaybookStep("If xpdp configurator then _include: ... else _include: ...");
        parentStep.setLineNumber(1);
        parentStep.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep sub1 = new PlaybookStep("Open configurator modal");
        sub1.setLineNumber(2);
        sub1.setStatus(PlaybookStepStatus.SUCCESS);
        sub1.setParent(parentStep);

        final PlaybookStep sub2 = new PlaybookStep("Add to cart from configurator");
        sub2.setLineNumber(3);
        sub2.setStatus(PlaybookStepStatus.SUCCESS);
        sub2.setParent(parentStep);

        parentStep.setSubSteps(List.of(sub1, sub2));
        context.getTransientData().put("playbook.steps", List.of(parentStep));

        final TestExecutionReport report = new TestExecutionReport();
        final ReportStepEntry repStep = new ReportStepEntry(0, "If xpdp configurator then _include: ... else _include: ...");
        repStep.setStatus("SUCCESS");

        final ReportActionEntry act1 = new ReportActionEntry("CLICK", "#config-btn", null, "Click config", "Open modal", true);
        act1.setSubStepIndex(0);
        act1.setSubStepInstruction("Open configurator modal");

        final ReportActionEntry act2 = new ReportActionEntry("CLICK", "#add-to-cart", null, "Click add", "Add item", true);
        act2.setSubStepIndex(1);
        act2.setSubStepInstruction("Add to cart from configurator");

        repStep.addAction(act1);
        repStep.addAction(act2);

        final ReportStepEntry repSub1 = new ReportStepEntry(0, "Open configurator modal");
        repSub1.setStatus("SUCCESS");
        repSub1.addAction(act1);

        final ReportStepEntry repSub2 = new ReportStepEntry(1, "Add to cart from configurator");
        repSub2.setStatus("SUCCESS");
        repSub2.addAction(act2);

        repStep.addSubStep(repSub1);
        repStep.addSubStep(repSub2);
        report.addStep(repStep);

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, runId, 1, "passed", null, report);
        engine.pushState(stateJson);

        final File logFile = new File(tempResultsDir, "console-execution-1.json");
        assertTrue(logFile.exists(), "console-execution-1.json should exist on disk");

        final String loggedJson = Files.readString(logFile.toPath());
        final JsonNode loggedRoot = new ObjectMapper().readTree(loggedJson);

        final JsonNode stepsNode = loggedRoot.path("blocks").path("steps");
        assertEquals(1, stepsNode.size());

        final JsonNode stepNode = stepsNode.get(0);
        final JsonNode parentActions = stepNode.path("actions");
        assertEquals(2, parentActions.size(), "Parent step must have 2 actions");

        assertEquals(0, parentActions.get(0).path("subStepIndex").asInt());
        assertEquals("Open configurator modal", parentActions.get(0).path("subStepInstruction").asText());
        assertEquals("#config-btn", parentActions.get(0).path("target").asText());

        assertEquals(1, parentActions.get(1).path("subStepIndex").asInt());
        assertEquals("Add to cart from configurator", parentActions.get(1).path("subStepInstruction").asText());
        assertEquals("#add-to-cart", parentActions.get(1).path("target").asText());

        final JsonNode subSteps = stepNode.path("subSteps");
        assertEquals(2, subSteps.size(), "Parent step must have 2 subSteps");

        final JsonNode sub1Node = subSteps.get(0);
        assertEquals(1, sub1Node.path("actions").size(), "Substep 1 must have its own action");
        assertEquals("#config-btn", sub1Node.path("actions").get(0).path("target").asText());

        final JsonNode sub2Node = subSteps.get(1);
        assertEquals(1, sub2Node.path("actions").size(), "Substep 2 must have its own action");
        assertEquals("#add-to-cart", sub2Node.path("actions").get(0).path("target").asText());
    }

    @Test
    public void testBuildStateJsonIncludesContextLevelsForInFlightStep() throws Exception
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PlaybookStep pbStep = new PlaybookStep("Navigate to homepage");
        pbStep.setContextLevel("LEAN");
        context.getTransientData().put("playbook.steps", List.of(pbStep));

        final StepStats stats = new StepStats("Navigate to homepage", 1000L);
        stats.addContextLevel("LEAN");
        context.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

        final TestExecutionReport report = new TestExecutionReport();
        final ReportStepEntry repStep = new ReportStepEntry(0, "Navigate to homepage");
        repStep.setStatus("RUNNING");
        report.addStep(repStep);

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run-ctx", 0, "running", null, report);
        final JsonNode loggedRoot = new ObjectMapper().readTree(stateJson);
        final JsonNode stepNode = loggedRoot.path("blocks").path("steps").get(0);

        assertEquals("LEAN", stepNode.path("contextLevel").asText());
        assertEquals("LEAN", stepNode.path("contextLevels").asText());
        assertEquals("LEAN", stepNode.path("stats").path("contextLevel").asText());
        assertEquals("LEAN", stepNode.path("stats").path("contextLevels").asText());

        // Escalated step
        final PlaybookStep pbStep1 = new PlaybookStep("Configure item");
        pbStep1.setContextLevel("STANDARD");
        context.getTransientData().put("playbook.steps", List.of(pbStep, pbStep1));

        final StepStats stats1 = new StepStats("Configure item", 2000L);
        stats1.addContextLevel("LEAN");
        stats1.addContextLevel("STANDARD");
        context.getTransientData().put("KEY_CURRENT_STEP_STATS", stats1);

        final ReportStepEntry repStep1 = new ReportStepEntry(1, "Configure item");
        repStep1.setStatus("RUNNING");
        report.addStep(repStep1);

        final String stateJson1 = InteractiveStateBuilder.buildStateJson(null, context, "run-ctx", 1, "running", null, report);
        final JsonNode loggedRoot1 = new ObjectMapper().readTree(stateJson1);
        final JsonNode stepNode1 = loggedRoot1.path("blocks").path("steps").get(1);

        assertEquals("STANDARD", stepNode1.path("contextLevel").asText());
        assertEquals("LEAN → STANDARD", stepNode1.path("contextLevels").asText());
        assertEquals("STANDARD", stepNode1.path("stats").path("contextLevel").asText());
        assertEquals("LEAN → STANDARD", stepNode1.path("stats").path("contextLevels").asText());
        assertEquals(1, stepNode1.path("stats").path("escalations").asInt());
    }

    @Test
    public void testPushStateRunningStatusPurgesPauseIdAndClearsCurrentPauseId() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run-pause-test");
        engine.registerPauseId("pre-registered-pause-token");
        assertEquals("pre-registered-pause-token", engine.getCurrentPauseId());

        final String runningStateJson = """
            {
                "runId": "run-pause-test",
                "status": "running",
                "pauseId": "stale-token-123"
            }
            """;
        engine.pushState(runningStateJson);

        assertNull(engine.getCurrentPauseId(), "currentPauseId must be cleared when state is running");

        final JsonNode loggedRoot = new ObjectMapper().readTree(engine.getCurrentStateJson());
        assertFalse(loggedRoot.has("pauseId"), "pauseId property must be stripped from running state payload");
    }

    @Test
    public void testPushStateCompletedStatusWithoutPauseIdPurgesPauseId() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run-pause-test");
        for (final String status : List.of("passed", "failed", "aborted"))
        {
            engine.registerPauseId("token-" + status);
            assertEquals("token-" + status, engine.getCurrentPauseId());

            final String stateJson = String.format("{\"runId\":\"run-pause-test\",\"status\":\"%s\"}", status);
            engine.pushState(stateJson);

            assertNull(engine.getCurrentPauseId(), "currentPauseId must be cleared when status is " + status);
            final JsonNode root = new ObjectMapper().readTree(engine.getCurrentStateJson());
            assertFalse(root.has("pauseId"), "pauseId must be purged when status is " + status);
        }
    }

    @Test
    public void testPushStateCompletedStatusWithExplicitPauseIdPreservesPauseId() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run-pause-test");
        final String stateJson = "{\"runId\":\"run-pause-test\",\"status\":\"passed\",\"pauseId\":\"pause-final-123\"}";
        engine.pushState(stateJson);

        assertEquals("pause-final-123", engine.getCurrentPauseId());
        final JsonNode root = new ObjectMapper().readTree(engine.getCurrentStateJson());
        assertEquals("pause-final-123", root.path("pauseId").asText());
    }

    @Test
    public void testWaitForActionClearsCurrentPauseIdOnCompletion() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run-wait-test");
        final String pauseId = "pause-wait-123";
        engine.registerPauseId(pauseId);
        assertEquals(pauseId, engine.getCurrentPauseId());

        final Thread actionThread = new Thread(() ->
        {
            try
            {
                Thread.sleep(100);
                final JsonObject req = new JsonObject();
                req.addProperty("runId", "run-wait-test");
                req.addProperty("pauseId", pauseId);
                req.addProperty("action", "RUN");
                engine.submitAction(req);
            }
            catch (final InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
        });
        actionThread.start();

        final JsonObject result = engine.waitForAction(pauseId, 5000);
        assertEquals("RUN", result.get("action").getAsString());
        assertNull(engine.getCurrentPauseId(), "currentPauseId must be null after waitForAction completes");

        actionThread.join(2000);
    }

    @Test
    public void testPendingStepBeforeActiveStepIndexIsNotCoercedToSuccess() throws Exception
    {
        final ExecutionContext context = new ExecutionContext(null);
        final PlaybookStep step0 = new PlaybookStep("Step 0 - Home");
        step0.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep step1 = new PlaybookStep("Step 1 - Inserted");
        step1.setStatus(PlaybookStepStatus.PENDING);

        final PlaybookStep step2 = new PlaybookStep("Step 2 - Login");
        step2.setStatus(PlaybookStepStatus.RUNNING);

        context.getTransientData().put("playbook.steps", List.of(step0, step1, step2));

        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run-step-pending-test", 2, "paused", "pause-token-123", null);
        final JsonNode root = new ObjectMapper().readTree(stateJson);
        final JsonNode stepsNode = root.path("blocks").path("steps");

        assertEquals(3, stepsNode.size());
        assertEquals("passed", stepsNode.get(0).path("status").asText(), "Step 0 must be reported as passed");
        assertEquals("pending", stepsNode.get(1).path("status").asText(), "Inserted pending step before activeStepIndex must NOT be coerced to passed");
        assertEquals("running", stepsNode.get(2).path("status").asText(), "Step 2 must be reported as running");
    }

    @Test
    public void testReorderActionAcceptedEvenWhenPauseIdIsNull()
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run-reorder-null-pause");
        assertNull(engine.getCurrentPauseId(), "Engine must initially have null pauseId");

        final JsonObject reorderReq = new JsonObject();
        reorderReq.addProperty("runId", "run-reorder-null-pause");
        reorderReq.addProperty("action", "REORDER");
        reorderReq.addProperty("fromBlock", "steps");
        reorderReq.addProperty("fromIndex", 1);
        reorderReq.addProperty("toBlock", "steps");
        reorderReq.addProperty("toIndex", 2);

        final ActionResult result = engine.submitAction(reorderReq);
        assertEquals(200, result.statusCode());
        assertTrue(result.responseBody().contains("\"status\":\"accepted\""),
            "REORDER action must be accepted even when activePauseId is null. Got: " + result.responseBody());

        // Now set an active pause token, but submit REORDER with mismatched/old pauseId
        engine.registerPauseId("new-pause-token-456");
        final JsonObject mismatchedReorderReq = new JsonObject();
        mismatchedReorderReq.addProperty("runId", "run-reorder-null-pause");
        mismatchedReorderReq.addProperty("pauseId", "old-stale-pause-token");
        mismatchedReorderReq.addProperty("action", "REORDER");
        mismatchedReorderReq.addProperty("fromIndex", 0);
        mismatchedReorderReq.addProperty("toIndex", 1);

        final ActionResult mismatchedResult = engine.submitAction(mismatchedReorderReq);
        assertEquals(200, mismatchedResult.statusCode());
        assertTrue(mismatchedResult.responseBody().contains("\"status\":\"accepted\""),
            "REORDER action must be accepted even when incoming pauseId is mismatched/stale. Got: " + mismatchedResult.responseBody());

        // Contrast: execution-advancing action without matching pauseId must still return already-handled
        final JsonObject runReq = new JsonObject();
        runReq.addProperty("runId", "run-reorder-null-pause");
        runReq.addProperty("pauseId", "old-stale-pause-token");
        runReq.addProperty("action", "RUN");

        final ActionResult runResult = engine.submitAction(runReq);
        assertEquals(200, runResult.statusCode());
        assertTrue(runResult.responseBody().contains("\"status\":\"already-handled\""),
            "RUN action with mismatched pauseId must return already-handled. Got: " + runResult.responseBody());
    }

    @Test
    public void testMultipleSequentialReordersAcceptedWithoutAlreadyHandledRejection() throws Exception
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("run-multi-reorder");
        final String pauseId = "pause-multi-123";
        engine.registerPauseId(pauseId);

        final JsonObject reorder1 = new JsonObject();
        reorder1.addProperty("runId", "run-multi-reorder");
        reorder1.addProperty("pauseId", pauseId);
        reorder1.addProperty("action", "REORDER");
        reorder1.addProperty("fromIndex", 1);
        reorder1.addProperty("toIndex", 2);

        final JsonObject reorder2 = new JsonObject();
        reorder2.addProperty("runId", "run-multi-reorder");
        reorder2.addProperty("pauseId", pauseId);
        reorder2.addProperty("action", "REORDER");
        reorder2.addProperty("fromIndex", 3);
        reorder2.addProperty("toIndex", 0);

        final ActionResult res1 = engine.submitAction(reorder1);
        assertEquals(200, res1.statusCode());
        assertTrue(res1.responseBody().contains("\"status\":\"accepted\""));

        final ActionResult res2 = engine.submitAction(reorder2);
        assertEquals(200, res2.statusCode());
        assertTrue(res2.responseBody().contains("\"status\":\"accepted\""),
            "Second sequential REORDER must not be rejected with already-handled. Got: " + res2.responseBody());

        // Verify FIFO consumption by waitForAction
        final JsonObject action1 = engine.waitForAction(pauseId, 1000);
        assertEquals("REORDER", action1.get("action").getAsString());
        assertEquals(1, action1.get("fromIndex").getAsInt());

        final JsonObject action2 = engine.waitForAction(pauseId, 1000);
        assertEquals("REORDER", action2.get("action").getAsString());
        assertEquals(3, action2.get("fromIndex").getAsInt());
    }
}
