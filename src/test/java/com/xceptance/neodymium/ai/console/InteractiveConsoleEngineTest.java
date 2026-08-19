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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.config.AiConfiguration;

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
    public void testPushStateExtractsExecutionKeyAndIncrementsIndexes() throws IOException
    {
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        AiConfiguration.resetInstance();

        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-run-unit");

        engine.pushState("{\"testName\":\"AlphaTest\",\"status\":\"passed\"}");
        engine.pushState("{\"testName\":\"BetaTest\",\"status\":\"passed\"}");

        final File log1 = new File(tempResultsDir, "console-execution-1.json");
        final File log2 = new File(tempResultsDir, "console-execution-2.json");

        assertTrue(log1.exists(), "console-execution-1.json should exist for AlphaTest");
        assertTrue(log2.exists(), "console-execution-2.json should exist for BetaTest");

        final String content1 = Files.readString(log1.toPath());
        final String content2 = Files.readString(log2.toPath());

        assertTrue(content1.contains("AlphaTest"), "log1 should contain AlphaTest");
        assertTrue(content2.contains("BetaTest"), "log2 should contain BetaTest");
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
}
