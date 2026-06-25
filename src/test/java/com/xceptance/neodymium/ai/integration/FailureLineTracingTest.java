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
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.util.YamlFileReader;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xceptance.neodymium.ai.core.AiAgent.DefinitiveAssertionError;

/**
 * Integration tests verifying that step failure messages accurately trace
 * line numbers and file names for both regular steps and included fragment steps,
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("featuretest")
@Tag("integration")
@Tag("llm")
public final class FailureLineTracingTest extends BaseAiTest
{
    @TempDir
    File tempDir;

    /**
     * Overrides the current test SUT page URL to point to our dedicated failure SUT page.
     */
    @BeforeEach
    public final void setupUrl()
    {
        useTempPlaybookDirectory();
        currentTestUrl = String.format("http://localhost:%d/FailureLineTracingTest/testFailure.html", server.getPort());
        Neodymium.getData().put("test.url", currentTestUrl);
    }

    /**
     * Verifies that a failure during LIVE_LLM execution of a regular step yields
     * an AssertionError message with the correct line number and main playbook file name.
     *
     * @throws Exception if YAML file creation or execution fails
     */
    @NeodymiumTest
    public final void testRegularLlmFailure() throws Exception
    {
        final String mainContent = """
            steps:
              - OPEN ${test.url}
              - Verify that the title contains "NonExistentTitle"
            data:
              - _testId: TC_LlmFail""";
        setupTestPlaybook(mainContent);

        final Throwable error = assertThrows(Throwable.class, () ->
        {
            runAi(VerificationMode.LIVE_LLM);
        });

        final String msg = error.getMessage() != null && error.getMessage().contains("Instruction")
                ? error.getMessage()
                : (error.getCause() != null ? error.getCause().getMessage() : "");
        assertNotNull(msg);
        assertTrue(msg.contains("Verify that the title contains \"NonExistentTitle\""));
        assertTrue(msg.contains("main.yaml"));
        assertTrue(msg.contains("line 3") || msg.contains("main.yaml:3"));

        final DefinitiveAssertionError defError = error instanceof DefinitiveAssertionError
                ? (DefinitiveAssertionError) error
                : (error.getCause() instanceof DefinitiveAssertionError ? (DefinitiveAssertionError) error.getCause() : null);
        assertNotNull(defError);
        assertEquals("Verify that the title contains \"NonExistentTitle\"", defError.getInstruction());
        assertEquals("main.yaml:3", defError.getLineNumber());
        assertTrue(defError.getSourceFile().endsWith("main.yaml"));
    }

    /**
     * Verifies that a failure during REPLAY execution of a regular step yields
     * an AssertionError message with the correct line number and main playbook file name.
     *
     * @throws Exception if YAML file creation or execution fails
     */
    @NeodymiumTest
    public final void testRegularReplayFailure() throws Exception
    {
        final String mainContent = """
            steps:
              - OPEN ${test.url}
              - Click on the 'Click Me' button
            data:
              - _testId: TC_ReplayFail""";
        setupTestPlaybook(mainContent);

        // Delete any leftover playbooks to record fresh
        final File playbookFile = PlaybookManager.getPlaybookFile(Neodymium.getTestName());
        if (playbookFile.exists())
        {
            playbookFile.delete();
        }

        // 1. LIVE_LLM run - must succeed to save the playbook
        runAi(VerificationMode.LIVE_LLM);

        // 2. Modify state by setting a query parameter that hides the click button
        Neodymium.getData().put("test.url", currentTestUrl + "?fail=true");
        this.resetBrowser();

        // 3. REPLAY run - must fail at step 2 (Click button)
        final Throwable error = assertThrows(Throwable.class, () ->
        {
            runAi(VerificationMode.REPLAY);
        });

        final String msg = error.getMessage() != null && error.getMessage().contains("Instruction")
                ? error.getMessage()
                : (error.getCause() != null ? error.getCause().getMessage() : "");
        assertNotNull(msg);
        assertTrue(msg.contains("Click on the 'Click Me' button"));
        assertTrue(msg.contains("main.yaml"));
        assertTrue(msg.contains("line 3") || msg.contains("main.yaml:3"));

        final DefinitiveAssertionError defError = error instanceof DefinitiveAssertionError
                ? (DefinitiveAssertionError) error
                : (error.getCause() instanceof DefinitiveAssertionError ? (DefinitiveAssertionError) error.getCause() : null);
        assertNotNull(defError);
        assertEquals("Click on the 'Click Me' button", defError.getInstruction());
        assertEquals("main.yaml:3", defError.getLineNumber());
        assertTrue(defError.getSourceFile().endsWith("main.yaml"));
    }

    /**
     * Verifies that a failure during LIVE_LLM execution of an included step yields
     * an AssertionError message with the correct line number and included fragment file name.
     *
     * @throws Exception if YAML file creation or execution fails
     */
    @NeodymiumTest
    public final void testIncludeLlmFailure() throws Exception
    {
        final String mainContent = """
            steps:
              - OPEN ${test.url}
              - _include: fragments/failure.steps
            data:
              - _testId: TC_IncLlmFail""";
        final String includeContent = """
            - Click on the 'Click Me' button
            - Verify that the title contains "NonExistentTitle"
            """;
        setupIncludeTestPlaybooks(mainContent, "fragments/failure.steps", includeContent);

        final Throwable error = assertThrows(Throwable.class, () ->
        {
            runAi(VerificationMode.LIVE_LLM);
        });

        final String msg = error.getMessage() != null && error.getMessage().contains("Instruction")
                ? error.getMessage()
                : (error.getCause() != null ? error.getCause().getMessage() : "");
        assertNotNull(msg);
        assertTrue(msg.contains("Verify that the title contains \"NonExistentTitle\""));
        assertTrue(msg.contains("failure.steps"));
        assertTrue(msg.contains("line 2") || msg.contains("failure.steps:2"));

        final DefinitiveAssertionError defError = error instanceof DefinitiveAssertionError
                ? (DefinitiveAssertionError) error
                : (error.getCause() instanceof DefinitiveAssertionError ? (DefinitiveAssertionError) error.getCause() : null);
        assertNotNull(defError);
        assertEquals("Verify that the title contains \"NonExistentTitle\"", defError.getInstruction());
        assertEquals("failure.steps:2 -> main.yaml:3", defError.getLineNumber());
        assertTrue(defError.getSourceFile().endsWith("main.yaml"));
    }

    /**
     * Verifies that a failure during REPLAY execution of an included step yields
     * an AssertionError message with the correct line number and included fragment file name.
     *
     * @throws Exception if YAML file creation or execution fails
     */
    @NeodymiumTest
    public final void testIncludeReplayFailure() throws Exception
    {
        final String mainContent = """
            steps:
              - OPEN ${test.url}
              - _include: fragments/success_to_fail.steps
            data:
              - _testId: TC_IncReplayFail""";
        final String includeContent = """
            - Click on the 'Click Me' button
            """;
        setupIncludeTestPlaybooks(mainContent, "fragments/success_to_fail.steps", includeContent);

        // Delete any leftover playbooks to record fresh
        final File playbookFile = PlaybookManager.getPlaybookFile(Neodymium.getTestName());
        if (playbookFile.exists())
        {
            playbookFile.delete();
        }

        // 1. LIVE_LLM run - must succeed to save the playbook
        runAi(VerificationMode.LIVE_LLM);

        // 2. Modify state by setting a query parameter that hides the click button
        Neodymium.getData().put("test.url", currentTestUrl + "?fail=true");
        this.resetBrowser();

        // 3. REPLAY run - must fail at the included step
        final Throwable error = assertThrows(Throwable.class, () ->
        {
            runAi(VerificationMode.REPLAY);
        });

        final String msg = error.getMessage() != null && error.getMessage().contains("Instruction")
                ? error.getMessage()
                : (error.getCause() != null ? error.getCause().getMessage() : "");
        assertNotNull(msg);
        assertTrue(msg.contains("Click on the 'Click Me' button"));
        assertTrue(msg.contains("success_to_fail.steps"));
        assertTrue(msg.contains("line 1") || msg.contains("success_to_fail.steps:1"));

        final DefinitiveAssertionError defError = error instanceof DefinitiveAssertionError
                ? (DefinitiveAssertionError) error
                : (error.getCause() instanceof DefinitiveAssertionError ? (DefinitiveAssertionError) error.getCause() : null);
        assertNotNull(defError);
        assertEquals("Click on the 'Click Me' button", defError.getInstruction());
        assertEquals("success_to_fail.steps:1 -> main.yaml:3", defError.getLineNumber());
        assertTrue(defError.getSourceFile().endsWith("main.yaml"));
    }

    private void setupTestPlaybook(final String content) throws IOException
    {
        final File mainYaml = new File(this.tempDir, "main.yaml");
        Files.writeString(mainYaml.toPath(), content, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(mainYaml);
        final Map<String, String> row = dataSets.get(0);
        for (final Map.Entry<String, String> entry : row.entrySet())
        {
            Neodymium.getData().put(entry.getKey(), entry.getValue());
        }
        Neodymium.getData().put("neodymium.sourceFile", mainYaml.getAbsolutePath());
    }

    private void setupIncludeTestPlaybooks(final String mainContent, final String includePath, final String includeContent) throws IOException
    {
        final File mainYaml = new File(this.tempDir, "main.yaml");
        Files.writeString(mainYaml.toPath(), mainContent, StandardCharsets.UTF_8);

        final File includeFile = new File(this.tempDir, includePath);
        includeFile.getParentFile().mkdirs();
        Files.writeString(includeFile.toPath(), includeContent, StandardCharsets.UTF_8);

        final List<Map<String, String>> dataSets = YamlFileReader.readFile(mainYaml);
        final Map<String, String> row = dataSets.get(0);
        for (final Map.Entry<String, String> entry : row.entrySet())
        {
            Neodymium.getData().put(entry.getKey(), entry.getValue());
        }
        Neodymium.getData().put("neodymium.sourceFile", mainYaml.getAbsolutePath());
    }
}
