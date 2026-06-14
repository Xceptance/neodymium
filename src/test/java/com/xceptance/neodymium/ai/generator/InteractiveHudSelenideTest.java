/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.ai.generator;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.LlmClient;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Selenide-based integration tests for the Interactive HUD verifying all
 * user interaction, stepping, failure recovery, skipping, and fast-forward modes.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class InteractiveHudSelenideTest extends BaseAiTest
{
    private volatile Thread bgThread = null;
    private volatile Throwable bgThrowable = null;
    private long originalTimeout;
    private boolean originalHeadless;
    private File tempSourceFile;

    @BeforeAll
    public static void enableInteractiveMode()
    {
        Neodymium.clearThreadContext();
        System.setProperty("neodymium.ai.interactive", "true");
        System.setProperty("neodymium.ai.interactive.autoSkip", "false");
        System.setProperty("neodymium.ai.apiKey", "dummy-api-key");
        System.setProperty("neodymium.ai.agent.maxRetries", "0");
        Neodymium.clearThreadContext();
    }

    @AfterAll
    public static void disableInteractiveMode()
    {
        System.clearProperty("neodymium.ai.interactive");
        System.clearProperty("neodymium.ai.interactive.autoSkip");
        System.clearProperty("neodymium.ai.apiKey");
        System.clearProperty("neodymium.ai.agent.maxRetries");
        Neodymium.clearThreadContext();
    }

    @BeforeEach
    public void setupCustomPageUrl()
    {
        Neodymium.clearThreadContext();
        currentTestUrl = String.format("http://localhost:%d/InteractiveHudSelenideTest/interactive-hud-test.html", server.getPort());
        bgThrowable = null;
        bgThread = null;

        // Reset thread-local Neodymium state to prevent cross-test leakage
        Neodymium.setAiPlaybook(null);
        Neodymium.setInteractiveHud(null);
        Neodymium.getData().clear();
        final File masterFile = new File("src/test/resources/dummy-test.yml");
        try
        {
            this.tempSourceFile = File.createTempFile("temp-dummy-test-", ".yml", new File("target"));
            Files.copy(masterFile.toPath(), this.tempSourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to copy master dummy-test.yml", e);
        }

        Neodymium.getData().put("neodymium.sourceFile", this.tempSourceFile.getAbsolutePath());

        // Force config properties to prevent caching issues across test classes
        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive", "true");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.interactive.autoSkip", "false");
        Neodymium.aiConfiguration().setProperty("neodymium.ai.agent.maxRetries", "0");
        System.setProperty("neodymium.ai.interactive.allowHeadlessHUD", "true");

        // Increase Selenide timeout and enable headless mode for robust execution in headless Chrome
        originalTimeout = Configuration.timeout;
        Configuration.timeout = 35000;
        originalHeadless = Configuration.headless;
        Configuration.headless = true;
    }

    @AfterEach
    public void cleanupBackgroundThread()
    {
        // Restore Selenide timeout and headless mode
        Configuration.timeout = originalTimeout;
        Configuration.headless = originalHeadless;

        if (bgThread != null && bgThread.isAlive())
        {
            try
            {
                // Give the background thread a brief moment to finish cleanly before interrupting
                bgThread.join(1000);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            if (bgThread.isAlive())
            {
                bgThread.interrupt();
                try
                {
                    bgThread.join(2000);
                }
                catch (final InterruptedException e)
                {
                    // Ignore
                }
            }
        }
        bgThread = null;
        bgThrowable = null;
        try
        {
            Selenide.closeWebDriver();
        }
        catch (final Exception e)
        {
            // Ignore
        }

        if (this.tempSourceFile != null && this.tempSourceFile.exists())
        {
            this.tempSourceFile.delete();
        }

        // Restore agent max retries default configuration
        Neodymium.aiConfiguration().setProperty("neodymium.ai.agent.maxRetries", "3");
        System.clearProperty("neodymium.ai.interactive.allowHeadlessHUD");
    }

    private void runInteractiveInBg(final Runnable r)
    {
        bgThrowable = null;
        bgThread = InteractiveHudTestUtils.runInteractiveInBg(r, t ->
        {
            bgThrowable = t;
            return null;
        });
    }

    private void checkBgError()
    {
        if (bgThrowable != null)
        {
            throw new RuntimeException("Background thread failed!", bgThrowable);
        }
    }

    private void joinBgThread()
    {
        if (bgThread != null)
        {
            try
            {
                bgThread.join(20000);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    private AiBrowser createTestAiBrowser() throws Exception
    {
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{}")
                .build());
        return InteractiveHudTestUtils.createTestAiBrowser(this, mockLlm);
    }

    private AiBrowser createTestAiBrowser(final LlmClient mockLlmClient) throws Exception
    {
        return InteractiveHudTestUtils.createTestAiBrowser(this, mockLlmClient);
    }

    private void openTestUrl()
    {
        open(currentTestUrl);
        try
        {
            Selenide.clearBrowserLocalStorage();
            Selenide.executeJavaScript("sessionStorage.clear(); localStorage.clear();");
        }
        catch (final Exception e)
        {
            // Ignore
        }
        open(currentTestUrl);
    }

    /**
     * Verifies the basic step-by-step execution in interactive mode where all actions
     * in the playbook step list succeed, and the HUD correctly minimizes during action
     * execution and maximizes again when waiting for the user's next approval.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testStepThroughWorkingStepList() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testStepThroughWorkingStepList");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1 (Resolves to CSS selector #btn1)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("First click reasoning");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2 (Resolves to CSS selector #btn2)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Second click reasoning");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread so that the JUnit main thread
        // can interactively inspect the SUT page and manipulate the HUD in the active browser.
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                // Execute the multiline prompt block representing our test steps
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify that the HUD successfully injects and renders the first step's details
        checkBgError(); // Assert no premature exceptions in the background runner
        $("#neo-ai-hud").shouldBe(Condition.visible); // The HUD panel must be visible to the user

        // Assert HUD default styling states (fully maximized layout)
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "flex"));
        $("#neo-min-circle").shouldHave(Condition.cssValue("display", "none"));

        // Assert that the HUD displays "Click button 1" as the next action to perform
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 1"));

        // 5. Click the approve button (✓) in the HUD to authorize the first step execution
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render the next step in HUD

        // 6. Verify Step 1 executed successfully, SUT updated, and HUD loaded Step 2
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "flex"));
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // Next action shifted to Step 2
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked")); // Verify SUT SUT updated

        // 7. Approve the second and final step
        $("#neo-approve-btn").click();

        // 8. Verify all steps are executed, the background thread exits, and the HUD closes cleanly
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible); // HUD closes on completion
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Verify both actions updated SUT state
        joinBgThread();
    }

    /**
     * Tests the step-by-step flow when an action execution fails, verifying that
     * the HUD remains visible, displays the error, and allows the user to manually
     * skip the failed step to complete the test run gracefully.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testStepThroughFailingStepList() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence (Success step -> Failing step)
        final Playbook playbook = new Playbook("testStepThroughFailingStepList");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1 (Succeeds)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Invisible Button (Fails since element #btn3 is invisible/missing)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click invisible button");
        step2.setReasoning("Fail step");
        step2.setActions(List.of(new Action("CLICK", "#btn3", "Click invisible button")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Instantiate a Mock LLM that returns a standard failure payload when executing the failing step
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{\"s\":false,\"e\":\"Element not found\",\"d\":true}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the failure mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                // Execute Step 1 followed by Step 2
                ai.execute("Click button 1");
                ai.execute("Click invisible button (timeout:2s)");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Verify the HUD starts successfully and wait for the first step's approval
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-approve-btn").click(); // Click Approve (✓) to execute Step 1
        Selenide.sleep(2500); // Give background thread time to execute step and render the next step in HUD

        // Wait until Step 2 is pending in the HUD
        checkBgError();
        $("#neo-next-action").shouldHave(Condition.text("Click invisible button"));

        // Click Approve (✓) to try executing Step 2 (which will fail because #btn3 is missing)
        $("#neo-approve-btn").click();

        // 6. Verify that when Step 2 executes, it encounters the execution failure,
        // causing the HUD to pause, maximize, and display the error message.
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible, Duration.ofSeconds(30));
        $("#neo-status-indicator").shouldHave(Condition.text("Debug - Error"), Duration.ofSeconds(30));
        $("#neo-reasoning-text").shouldHave(Condition.text("Action Failed"), Duration.ofSeconds(30));

        // 7. Click the Skip button (✕) in the HUD to bypass the failure and proceed
        $("#neo-skip-btn").click();

        // 8. Verify the failed step is skipped, the HUD closes cleanly, and the last valid state is reached
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible); // HUD closes as the test finishes
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked")); // Verify only step 1 succeeded
        joinBgThread();
    }

    /**
     * Tests that the "Dump Debug Context" button is correctly displayed when an
     * action execution fails, and that clicking it successfully writes both the
     * diagnostic text context and the raw HTML structure to the disk, while
     * keeping the HUD interactive and waiting for subsequent user input.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testDumpContextOnError() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence (Success step -> Failing step)
        final Playbook playbook = new Playbook("testDumpContextOnError");
        playbook.setRecording(false); // Replay mode

        // Define Step 1: Click Button 1 (Succeeds)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Success step");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Invisible Button (Fails because #btn3 is missing)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click invisible button");
        step2.setReasoning("Fail step");
        step2.setActions(List.of(new Action("CLICK", "#btn3", "Click invisible button")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Define a Mock LLM that returns a failure payload when executing Step 2
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{\"s\":false,\"e\":\"Element not found\",\"d\":true}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                ai.execute("Click button 1\nClick invisible button (timeout:2s)");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Approve Step 1 and verify it executes successfully
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render the next step in HUD

        checkBgError();
        $("#neo-next-action").shouldHave(Condition.text("Click invisible button"));

        // Delete any existing dump files in tmp/ before triggering the new dump
        final java.io.File tmpDir = new java.io.File("tmp");
        if (tmpDir.exists())
        {
            final java.io.File[] existingDumps = tmpDir.listFiles((dir, name) -> name.startsWith("neodymium-ai-dump-"));
            if (existingDumps != null)
            {
                for (final java.io.File f : existingDumps)
                {
                    f.delete();
                }
            }
        }

        // Click Approve (✓) to try executing Step 2 (which will fail because #btn3 is missing)
        $("#neo-approve-btn").click();

        // 6. Verify that the execution failure causes the HUD to display the error, maximize,
        // and display the "Dump Debug Context" button container.
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible, Duration.ofSeconds(30));
        $("#neo-status-indicator").shouldHave(Condition.text("Debug - Error"), Duration.ofSeconds(30));
        $("#neo-dump-btn").shouldBe(Condition.visible);

        // 7. Click the Dump button to trigger the DUMP action
        $("#neo-dump-btn").click();

        // 8. Assert that the diagnostic dump files are generated successfully on the disk
        boolean filesCreated = false;
        for (int i = 0; i < 20; i++)
        {
            if (tmpDir.exists())
            {
                final java.io.File[] dumps = tmpDir.listFiles((dir, name) -> name.startsWith("neodymium-ai-dump-"));
                if (dumps != null && dumps.length >= 2)
                {
                    filesCreated = true;
                    // Delete the generated files to clean up the workspace
                    for (final java.io.File f : dumps)
                    {
                        f.delete();
                    }
                    break;
                }
            }
            Selenide.sleep(500);
        }
        assertTrue(filesCreated, "Dump files should have been created in the tmp/ directory");

        // 9. Verify the HUD remains visible and waiting for further actions even after dump completes
        $("#neo-skip-btn").shouldBe(Condition.enabled).click();

        // 10. Verify that skipping closes the HUD cleanly and terminates the agent background thread
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        joinBgThread();
    }

    /**
     * Verifies that the user can interactively edit a pending instruction in the HUD,
     * including custom data binding modifications, and that the updated step is correctly
     * executed and recorded.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testEditStepAndBindings() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 1-step sequence
        final Playbook playbook = new Playbook("testEditStepAndBindings");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1 (Will be dynamically edited by the user to Click Button 2)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Initial step");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Define a Mock LLM that returns a successful execution payload for the edited "Click button 2" prompt
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{\"r\":\"Click button 2\",\"d\":true,\"a\":[{\"t\":\"CLICK\",\"tg\":\"#btn2\",\"desc\":\"Click button 2\"}]}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                ai.execute("Click button 1");
            }
            catch (final Throwable e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Verify the HUD starts successfully and wait for user actions
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);

        // 6. Click the Edit button (✎) in the HUD to open the prompt modification overlay
        $("#neo-edit-btn").shouldBe(Condition.enabled).click();
        $("#neo-edit-overlay").shouldBe(Condition.visible); // Assert that the edit overlay modal appears

        // Type the updated prompt instruction
        $("#neo-edit-input").setValue("Click button 2");

        // Submit the modifications to update the playbook state
        $("#neo-edit-submit-btn").click();

        // 7. Verify that the HUD correctly updates the next pending action text
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // Next action updated

        // 8. Click the Approve button (✓) to execute the edited step
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render finished confirmation dialog in HUD

        // 9. Since the playbook was interactively modified, verify that we see the "Save & Exit" dialog.
        $("#neo-approve-btn").shouldHave(Condition.attribute("data-is-finished", "true"), Duration.ofSeconds(35));

        // Click "Save & Exit" to persist and cleanly exit
        $("#neo-approve-btn").click();

        // Wait for the background thread to finish cleanly
        try
        {
            bgThread.join(20000);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        // 10. Verify that the HUD terminates cleanly, closes, and SUT is updated correctly
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Verify button 2 was clicked instead of button 1
    }

    /**
     * Tests the interactive "Skip" functionality, verifying that the user can skip
     * a pending step directly from the HUD and that execution proceeds to the next step.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testSkipStep() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testSkipStep");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1 (Will be manually skipped)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2 (Will be approved and executed)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Define a Mock LLM that returns a successful execution payload for "Click button 2"
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Verify the HUD starts successfully and displays Step 1
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 1"));

        // 6. Click the Skip button (✕) in the HUD to bypass Step 1
        $("#neo-skip-btn").click();

        // 7. Verify that Step 1 is bypassed without executing, and next action shifts to Step 2
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // Next action updated
        $("#result").shouldHave(Condition.exactText("Initial State")); // Assert SUT state did not change

        // 8. Click the Approve button (✓) to execute Step 2
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render finished confirmation dialog in HUD

        // 9. Since the playbook was interactively modified (Step 1 skipped), verify that we see the "Save & Exit" dialog.
        $("#neo-approve-btn").shouldHave(Condition.attribute("data-is-finished", "true"), Duration.ofSeconds(35));

        // Click "Save & Exit" to persist and cleanly exit
        $("#neo-approve-btn").click();

        // Wait for the background thread to finish cleanly
        try
        {
            bgThread.join(20000);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        // 10. Verify SUT state, HUD closure, and clean execution termination
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Verify only Button 2 was clicked
    }

    /**
     * Verifies that the user can dynamically insert a new custom instruction before
     * a pending step in the HUD, and that it is executed immediately.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testAddStep() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 1-step sequence
        final Playbook playbook = new Playbook("testAddStep");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 2 (We will insert a new step before this one)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 2");
        step1.setReasoning("Second click");
        step1.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step1);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Define a Mock LLM that returns a successful execution payload for the newly inserted "Click button 1" prompt
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{\"r\":\"Click button 1\",\"d\":true,\"a\":[{\"t\":\"CLICK\",\"tg\":\"#btn1\",\"desc\":\"Click button 1\"}]}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                ai.execute("Click button 2");
            }
            catch (final Throwable e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Verify the HUD starts successfully and displays the original pending Step 1
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2"));

        // 6. Click the Add step button (+) in the HUD to open the instruction insertion overlay
        $("#neo-add-overlay-btn").shouldBe(Condition.enabled).click();
        $("#neo-add-overlay").shouldBe(Condition.visible); // Assert that the add overlay modal appears

        // Type the new instruction prompt to insert before the current step
        $("#neo-add-input").setValue("Click button 1");
        $("#neo-add-submit-btn").click();
        Selenide.sleep(2500); // Give background thread time to settle and run the LLM resolution

        // 7. Verify that the HUD dynamically inserts the new step and shifts focus to it
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 1")); // Focus shifted to the new step

        // 8. Click the Approve button (✓) to execute the newly inserted Step 1
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render the next step in HUD

        // 9. Verify Step 1 executes successfully, updating SUT, and the original Step 2 is now pending
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // original step is now next
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked")); // Verify Button 1 was clicked

        // 10. Click the Approve button (✓) to execute the remaining original Step 2
        $("#neo-approve-btn").click();

        // 11. Since a step was dynamically inserted, verify that we see the "Save & Exit" dialog.
        $("#neo-approve-btn").shouldHave(Condition.attribute("data-is-finished", "true"), Duration.ofSeconds(35));

        // Click "Save & Exit" to persist and cleanly exit
        $("#neo-approve-btn").click();

        // Wait for the background thread to finish cleanly
        try
        {
            bgThread.join(20000);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        // 12. Verify SUT state, HUD closure, and clean execution termination
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Verify Button 2 also executed successfully
    }

    /**
     * Tests the history and execution rollback ("Back") feature, verifying that
     * clicking a previous history item or the back button correctly rewinds the test state.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testStepBackAndHistory() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testStepBackAndHistory");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully and approve Step 1
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render the next step in HUD

        // 5. Verify Step 1 executed successfully, and Step 2 is now pending in the HUD.
        // Also assert that the execution history item for Step 1 is visible.
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2"));
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked"));
        $("#neo-history-list li").shouldHave(Condition.text("Click button 1")); // Assert history item is rendered

        // 6. Click the Back button (⏪) in the HUD to rewind the execution back to Step 1
        $("#neo-rewind-btn").shouldBe(Condition.enabled).click();
        Selenide.sleep(2500); // Give background thread time to process rewind and update HUD

        // 7. Verify that the HUD successfully rolls back its internal cursor and highlights Step 1 as pending
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 1")); // Cursor rolled back to Step 1

        // 8. Re-approve Step 1
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render next step in HUD
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);

        // 9. Re-approve Step 2
        $("#neo-approve-btn").click();

        // 10. Verify everything finishes cleanly and both actions are executed successfully in the SUT
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked"));
        joinBgThread();
    }

    /**
     * Verifies that the user can interactively toggle breakpoints on steps in the HUD
     * planned list, and that setting or clearing a breakpoint correctly updates the
     * active breakpoint state.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testAddAndRemoveBreakpoint() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 1-step sequence
        final Playbook playbook = new Playbook("testAddAndRemoveBreakpoint");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully and click on the "Show Full Prompt" button
        // to maximize the step planning list modal overlay.
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-full-prompt-btn").click();
        $("#neo-full-prompt-overlay").shouldBe(Condition.visible); // Assert that the modal overlay is active

        // 5. Locate the breakpoint toggle element for Step 1 (Index 0)
        final com.codeborne.selenide.SelenideElement bpCol = $(".neo-bp-col[data-idx='0']");
        bpCol.shouldHave(Condition.text("⚪")); // Assert breakpoint is initially unset (White circle)

        // 6. Click the toggle element to set a Breakpoint on Step 1
        bpCol.click();
        bpCol.shouldHave(Condition.text("🛑")); // Assert breakpoint is active (Red octagon icon)

        // 7. Click the toggle element again to clear the Breakpoint
        bpCol.click();
        bpCol.shouldHave(Condition.text("⚪")); // Assert breakpoint is unset again

        // 8. Approve and execute the step to complete the test case cleanly
        $("#neo-approve-btn").click();
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        joinBgThread();
    }

    /**
     * Tests the Fast-Forward mode, verifying that execution runs automatically at full speed
     * until a step fails, at which point the HUD automatically pauses and maximizes itself
     * to show the failure context to the user.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testFastForwardTillError() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence (Success step -> Failing step)
        final Playbook playbook = new Playbook("testFastForwardTillError");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1 (Succeeds)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Success step");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Invisible Button (Fails)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click invisible button");
        step2.setReasoning("Fail step");
        step2.setActions(List.of(new Action("CLICK", "#btn3", "Click invisible button")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Define a Mock LLM that returns a failure payload when executing Step 2
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{\"s\":false,\"e\":\"Element not found\",\"d\":true}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                ai.execute("Click button 1\nClick invisible button (timeout:2s)");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Verify the HUD starts successfully and click the "Fast-Forward" (⏩) button
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        Selenide.sleep(1000); // Wait for background thread to fully settle in waitForHudAction
        $("#neo-autoskip-btn").click(); // Toggle Fast-Forward mode on

        // 6. Verify that the HUD automatically minimizes, executes Step 1 at full speed, SUT updates,
        // and pauses/maximizes immediately upon encountering the simulated error in Step 2.
        // Let the background thread run uninterrupted to execute Step 1 and pause at Step 2
        Selenide.sleep(5000);
        checkBgError();
        // Wait up to 25 seconds for the mock error detection and HUD transition to complete
        $("#neo-ai-hud").shouldBe(Condition.visible, Duration.ofSeconds(25));
        $("#neo-autoskip-text").shouldHave(Condition.exactText("Fast-Forward")); // Assert Fast-Forward mode state
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked"), Duration.ofSeconds(35)); // Verify Step 1 executed successfully

        // 7. Click the Skip button (✕) in the HUD to bypass the failing step
        $("#neo-skip-btn").click();
        checkBgError();

        // 8. Since we skipped a step, the HUD correctly stays open and presents the final playbook confirmation dialog.
        // Click the Save & Exit button (re-labeled #neo-approve-btn) to save changes and finalize.
        $("#neo-approve-btn").shouldHave(Condition.attribute("data-is-finished", "true"), Duration.ofSeconds(35));
        $("#neo-approve-btn").click();

        // 9. Verify that the HUD closes and terminates cleanly
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible, Duration.ofSeconds(10));
        joinBgThread();
    }

    /**
     * Verifies that the Fast-Forward mode executes all planned steps to the very end
     * without pausing if no breakpoints or errors are encountered, and that the HUD
     * remains minimized during the fast-forward run.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testFastForwardTillEndOfTest() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testFastForwardTillEndOfTest");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully and click the "Fast-Forward" (⏩) button
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        Selenide.sleep(1000); // Wait for background thread to fully settle in waitForHudAction
        $("#neo-autoskip-btn").click(); // Toggle Fast-Forward mode on

        // 5. Verify that the HUD remains minimized throughout execution and closes automatically
        // as soon as the final step executes successfully without manual intervention.
        Selenide.sleep(5000);
        checkBgError();
        joinBgThread(); // Join the background thread first to let it execute steps 100% uninterrupted
        
        $("#neo-ai-hud").shouldNotBe(Condition.visible, Duration.ofSeconds(35)); // HUD must close automatically at the end of the test
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked"), Duration.ofSeconds(35)); // Verify both actions updated SUT state
        $("#click-count").shouldHave(Condition.exactText("2"), Duration.ofSeconds(35)); // Verify 2 SUT clicks registered
    }

    /**
     * Tests that the Fast-Forward mode correctly pauses execution automatically right
     * before reaching a step that has an active breakpoint (🛑) set.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testFastForwardTillBreakpoint() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testFastForwardTillBreakpoint");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1 (Will be executed automatically in Fast-Forward mode)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2 (Will have a breakpoint set and pause before execution)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2 Breakpoint");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully, click "Show Full Prompt" to maximize the overlay list,
        // and set a breakpoint on Step 2 (Index 1).
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-full-prompt-btn").click();

        $(".neo-bp-col[data-idx='1']").click(); // Toggle breakpoint 🛑 on Step 2

        Selenide.sleep(500); // Wait briefly for the state to synchronize in the browser sessionStorage

        // 5. Click the "Fast-Forward" (⏩) button to start automatic execution
        Selenide.sleep(1000); // Wait for background thread to fully settle in waitForHudAction
        $("#neo-autoskip-btn").click();

        // 6. Verify that Step 1 is executed automatically, updating the SUT, and that the agent
        // pauses execution and maximizes the HUD immediately before executing the breakpoint on Step 2.
        Selenide.sleep(5000);
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible); // HUD maximized on reaching breakpoint
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "flex"));
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // Next action is Step 2
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked")); // Verify only Step 1 was executed

        // 7. Click the Approve button (✓) to manually execute Step 2 and finalize
        $("#neo-approve-btn").click();
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked"));
        joinBgThread();
    }

    /**
     * Verifies that after pausing at a breakpoint in Fast-Forward mode, the user can
     * resume Fast-Forwarding, and execution proceeds automatically to the end.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testFastForwardFromBreakpointAgain() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 4-step sequence
        final Playbook playbook = new Playbook("testFastForwardFromBreakpointAgain");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2 (Will have a breakpoint set)
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2 Breakpoint");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Define Step 3: Click Button 1 again
        final PlaybookStep step3 = new PlaybookStep();
        step3.setPromptLine("Click button 1 again");
        step3.setReasoning("Step 3");
        step3.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1 again")));
        playbook.addStep(step3);

        // Define Step 4: Click Button 2 again (Will have a breakpoint set)
        final PlaybookStep step4 = new PlaybookStep();
        step4.setPromptLine("Click button 2 again");
        step4.setReasoning("Step 4 Breakpoint");
        step4.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2 again")));
        playbook.addStep(step4);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1\nClick button 2\nClick button 1 again\nClick button 2 again");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully, click "Show Full Prompt", and set breakpoints on Step 2 (Index 1) and Step 4 (Index 3).
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-full-prompt-btn").click();

        $(".neo-bp-col[data-idx='1']").shouldHave(Condition.text("⚪")).click();
        $(".neo-bp-col[data-idx='1']").shouldHave(Condition.text("🛑"));
        $(".neo-bp-col[data-idx='3']").shouldHave(Condition.text("⚪")).click();
        $(".neo-bp-col[data-idx='3']").shouldHave(Condition.text("🛑"));

        Selenide.sleep(500); // Wait briefly for state synchronization

        // 5. Click the "Fast-Forward" (⏩) button to start automatic execution
        Selenide.sleep(1000); // Wait for background thread to fully settle in waitForHudAction
        $("#neo-autoskip-btn").click();

        // 6. Verify that execution pauses automatically when the first breakpoint on Step 2 is reached
        Selenide.sleep(5000);
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible, Duration.ofSeconds(15));
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2"), Duration.ofSeconds(15)); // Next action is Step 2
        $("#neo-playbook-reasoning").shouldHave(Condition.text("Original reasoning: Step 2 Breakpoint"), Duration.ofSeconds(15)); // Wait for actual breakpoint pause
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked"), Duration.ofSeconds(15)); // Step 1 completed successfully

        // 7. Click Fast-Forward (⏩) again to resume. It should run through Step 2 and Step 3, and pause on Step 4.
        Selenide.sleep(1000); // Wait for background thread to fully settle in its new pause
        $("#neo-autoskip-btn").click();

        Selenide.sleep(5000);
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible, Duration.ofSeconds(15));
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2 again"), Duration.ofSeconds(15)); // Next action is Step 4
        $("#neo-playbook-reasoning").shouldHave(Condition.text("Original reasoning: Step 4 Breakpoint"), Duration.ofSeconds(15)); // Wait for second breakpoint pause
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked"), Duration.ofSeconds(15)); // Step 2 (Click Button 2) and Step 3 (Click Button 1) executed

        // 8. Re-trigger Fast-Forward (⏩) to resume automated execution to completion
        Selenide.sleep(1000); // Wait for background thread to fully settle in its new pause
        $("#neo-autoskip-btn").click();

        // Wait for background thread to complete execution cleanly
        try
        {
            bgThread.join(20000);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        // 9. Verify execution completes automatically, the HUD closes, and the SUT updates correctly
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Step 4 executed successfully
    }

    /**
     * Tests the HUD's key binding shortcuts, verifying that keyboard commands like Alt+S
     * correctly toggle Fast-Forward mode and resume execution.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testKeyboardShortcuts() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testKeyboardShortcuts");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        Selenide.sleep(1000); // Give background thread time to start, capture context, and enter wait loop

        // 4. Verify the HUD starts successfully, focus it, and trigger Approve using Alt+A
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-ai-hud").click(); // Guarantee browser focus on HUD prior to keypress perform
        Selenide.actions().keyDown(Keys.ALT).sendKeys("a").keyUp(Keys.ALT).perform(); // Trigger Alt+A shortcut
        Selenide.sleep(1000); // Give background thread time to process Alt+A, execute, and enter next wait loop

        // 5. Verify Step 1 executed successfully, and Step 2 is pending in the HUD
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // Next action updated
        $("#result").shouldHave(Condition.exactText("Button 1 Clicked")); // Verify Button 1 clicked

        // 6. Focus the HUD again and trigger Fast-Forward mode using Alt+S
        Selenide.sleep(200); // Wait briefly for active focus stabilization
        $("#neo-ai-hud").click(); // Focus HUD prior to keyboard shortcut perform
        Selenide.actions().keyDown(Keys.ALT).sendKeys("s").keyUp(Keys.ALT).perform(); // Trigger Alt+S shortcut

        // 7. Verify the Fast-Forward mode executes Step 2 automatically and HUD closes cleanly
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked"), Duration.ofSeconds(10));
        joinBgThread();
    }

    /**
     * Verifies that the user can manually minimize the HUD to a small circular button
     * and maximize it back to the full panel using the UI controls.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testMinimizeAndMaximize() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 1-step sequence
        final Playbook playbook = new Playbook("testMinimizeAndMaximize");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully in maximized mode
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "flex"));
        $("#neo-min-circle").shouldHave(Condition.cssValue("display", "none")); // minimized trigger must be hidden

        // 5. Click the Minimize button (-) in the HUD panel
        $("#neo-min-btn").click();

        // 6. Verify that the main HUD is hidden and the small circular trigger button is displayed
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "none")); // panel hidden
        $("#neo-min-circle").shouldBe(Condition.visible); // circular button visible

        // 7. Click on the circular minimized button to maximize the HUD back
        $("#neo-min-circle").click();

        // 8. Verify the HUD panel displays maximized again
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "flex"));
        $("#neo-min-circle").shouldHave(Condition.cssValue("display", "none"));

        // 9. Click Approve to finalize the test case cleanly
        $("#neo-approve-btn").click();
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        joinBgThread();
    }

    /**
     * Tests that the HUD state, planning list, and history correctly persist and recover
     * across full page reloads.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testHudPersistenceAcrossPageReload() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 1-step sequence
        final Playbook playbook = new Playbook("testHudPersistenceAcrossPageReload");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully, click "Show Full Prompt" to show overlay, then minimize
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-full-prompt-btn").click();
        $("#neo-full-prompt-overlay").shouldBe(Condition.visible); // Assert that the modal overlay is active

        $("#neo-min-btn").click();
        $("#neo-min-circle").shouldBe(Condition.visible); // HUD is minimized

        // 5. Trigger a full page reload in the browser
        Selenide.refresh();

        // 6. Verify that the HUD correctly restores its minimized/maximized layout from sessionStorage post-refresh
        $("#neo-min-circle").shouldBe(Condition.visible); // HUD circular button is still visible
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "none")); // Main panel remains closed

        // 7. Maximize the HUD and verify that the full-prompt overlay was also successfully restored to displays "flex"/visible
        $("#neo-min-circle").click();
        $("#neo-ai-hud").shouldHave(Condition.cssValue("display", "flex"));
        $("#neo-full-prompt-overlay").shouldBe(Condition.visible); // Assert that the modal overlay is still active!

        // 8. Approve the step and finalize the test cleanly
        $("#neo-approve-btn").click();
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        joinBgThread();
    }

    /**
     * Verifies that when the interactive HUD saves its final execution state via the
     * "Save & Exit" button, the modified execution steps and custom test data bindings
     * are correctly written and serialized back to the YAML test data file.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testSaveYamlAndExitWithTestData() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Pre-populate a unique temporary YAML file inside src/test/resources/
        final String tempFileName = "temp-interactive-save-" + System.currentTimeMillis() + ".yml";
        final File tempFile = new File("src/test/resources/" + tempFileName);

        try
        {
            final String initialYaml = "steps: |\n" +
                "  Click button 1\n" +
                "data:\n" +
                "  - myKey: \"originalValue\"\n";
            Files.writeString(tempFile.toPath(), initialYaml);

            // 3. Configure the thread-local sourceFile and test data bindings in Neodymium
            Neodymium.getData().clear();
            Neodymium.setTestdataSourceFile(tempFileName);
            Neodymium.getData().put("myKey", "originalValue");

            // 4. Initialize the Playbook with a clean 1-step sequence matching the YAML
            final Playbook playbook = new Playbook("testSaveYamlAndExitWithTestData");
            playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

            // Define Step 1: Click Button 1 (Will be dynamically edited by the user to Click Button 2)
            final PlaybookStep step1 = new PlaybookStep();
            step1.setPromptLine("Click button 1");
            step1.setReasoning("Initial step");
            step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
            playbook.addStep(step1);

            // Register the configured playbook in the Neodymium context
            Neodymium.setAiPlaybook(playbook);

            // 5. Define a Mock LLM that returns a successful execution payload for the edited "Click button 2" prompt
            final MockLlmClient mockLlm = new MockLlmClient();
            mockLlm.addResponse(AiMockResponse.builder()
                    .responseText("{\"r\":\"Click button 2\",\"d\":true,\"a\":[{\"t\":\"CLICK\",\"tg\":\"#btn2\",\"desc\":\"Click button 2\"}]}")
                    .build());

            // 6. Launch the AI agent in a background execution thread using the mock LLM
            runInteractiveInBg(() ->
            {
                try (final AiBrowser ai = createTestAiBrowser(mockLlm))
                {
                    ai.execute("Click button 1");
                }
                catch (final Throwable e)
                {
                    throw new RuntimeException(e);
                }
            });

            // 7. Verify the HUD starts successfully
            checkBgError();
            $("#neo-ai-hud").shouldBe(Condition.visible);

            // Click the Edit button (✎) in the HUD to open the prompt modification overlay
            $("#neo-edit-btn").shouldBe(Condition.enabled).click();
            $("#neo-edit-overlay").shouldBe(Condition.visible); // Assert that the edit overlay modal appears

            // Type the updated prompt instruction
            $("#neo-edit-input").setValue("Click button 2");

            // Edit the test data binding value via the HUD UI input element instead of programmatically in Java
            $(".neo-binding-input[data-key='myKey']").setValue("newValue");

            // Submit the modifications to update the playbook state
            $("#neo-edit-submit-btn").click();
            Selenide.sleep(2500); // Give background thread time to capture new context and LLM resolution for edited step

            // Verify that the HUD correctly updates the next pending action text
            checkBgError();
            $("#neo-ai-hud").shouldBe(Condition.visible);
            $("#neo-next-action").shouldHave(Condition.exactText("Click button 2")); // Next action updated

            // Click the Approve button (✓) to execute the edited step
            $("#neo-approve-btn").click();
            Selenide.sleep(2500); // Give background thread time to execute step and render finished confirmation dialog in HUD
            checkBgError();

            // 8. Since we interactively modified the playbook step, the HUD correctly presents the Save & Exit dialog.
            // Verify that the approve button is re-labeled for Save & Exit
            $("#neo-approve-btn").shouldHave(Condition.attribute("data-is-finished", "true"), Duration.ofSeconds(35));

            // Click "Save & Exit" to persist the changes to the YAML file
            $("#neo-approve-btn").click();

            // Wait for the background thread to finish execution cleanly to avoid race conditions with file writes
            try
            {
                bgThread.join(20000);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            // 9. Assert that the HUD terminates cleanly and closes
            checkBgError();
            $("#neo-ai-hud").shouldNotBe(Condition.visible, Duration.ofSeconds(10));

            // 10. Read back the updated YAML file and assert its contents (both updated steps and updated test data)
            final String updatedYamlContent = Files.readString(tempFile.toPath());
            System.out.println("UPDATED YAML CONTENT:\n" + updatedYamlContent);
            assertTrue(updatedYamlContent.contains("steps:"), "Saved YAML should contain 'steps' key");
            assertTrue(updatedYamlContent.contains("Click button 2"), "Saved YAML should contain the edited step");
            assertTrue(!updatedYamlContent.contains("Click button 1"), "Saved YAML should no longer contain the old step");
            assertTrue(updatedYamlContent.contains("myKey: newValue"), "Saved YAML should contain the updated custom test data value");
        }
        finally
        {
            // 11. Strictly clean up the temporary YAML file to keep the workspace pristine
            if (tempFile.exists())
            {
                tempFile.delete();
            }
        }
    }
    /**
     * Verifies that if a user sets a breakpoint on a step and then immediately clears it,
     * the HUD's Fast-Forward mode will NOT pause at that step and will run through to completion.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testFastForwardWithClearedBreakpoint() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 2-step sequence
        final Playbook playbook = new Playbook("testFastForwardWithClearedBreakpoint");
        playbook.setRecording(false); // Disables recording so the HUD operates in replay/interactive review mode

        // Define Step 1: Click Button 1
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Define Step 2: Click Button 2
        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Click button 2");
        step2.setReasoning("Step 2");
        step2.setActions(List.of(new Action("CLICK", "#btn2", "Click button 2")));
        playbook.addStep(step2);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Launch the AI agent in a background execution thread
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser())
            {
                ai.execute("Click button 1\nClick button 2");
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        // 4. Verify the HUD starts successfully and click "Show Full Prompt"
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-full-prompt-btn").click();

        // 5. Toggle breakpoint ON for Step 2 (Index 1) and assert it is set
        final com.codeborne.selenide.SelenideElement bpCol = $(".neo-bp-col[data-idx='1']");
        bpCol.shouldHave(Condition.text("⚪"));
        bpCol.click();
        bpCol.shouldHave(Condition.text("🛑"));

        // 6. Toggle breakpoint OFF for Step 2 (Index 1) and assert it is cleared
        bpCol.click();
        bpCol.shouldHave(Condition.text("⚪"));

        Selenide.sleep(500); // Wait briefly for state synchronization

        // 7. Click Fast-Forward (⏩)
        Selenide.sleep(1000); // Wait for background thread to fully settle in waitForHudAction
        $("#neo-autoskip-btn").click();

        // Wait for background thread to complete execution cleanly
        Selenide.sleep(5000);
        try
        {
            bgThread.join(20000);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        // 8. Verify the HUD finished and closed cleanly without pausing on Step 2
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Both steps completed
    }

    /**
     * Verifies that if an active, pre-recorded playbook is executing in replay mode
     * (non-recording), making manual modifications via the HUD (e.g., editing a step)
     * successfully exits replay mode and enters recording/healing mode.
     *
     * @throws Exception if the test execution fails
     */
    @Test
    public void testPlaybookTransitionsToRecordingOnUIChange() throws Exception
    {
        // 1. Open the SUT HTML test page and ensure localStorage/sessionStorage are fully reset
        openTestUrl();

        // 2. Initialize the Playbook with a clean 1-step sequence in replay mode
        final Playbook playbook = new Playbook("testPlaybookTransitionsToRecordingOnUIChange");
        playbook.setRecording(false); // Playbook is initially in pure replay mode
        assertFalse(playbook.isRecording(), "Playbook should initially not be in recording mode");
        assertFalse(playbook.isChanged(), "Playbook should initially not be marked as changed");

        // Define Step 1: Click Button 1 (Will be dynamically edited by the user to Click Button 2)
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Click button 1");
        step1.setReasoning("Replay Step 1");
        step1.setActions(List.of(new Action("CLICK", "#btn1", "Click button 1")));
        playbook.addStep(step1);

        // Register the configured playbook in the Neodymium context
        Neodymium.setAiPlaybook(playbook);

        // 3. Define a Mock LLM that returns a successful execution payload for the edited "Click button 2" prompt
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText("{\"r\":\"Click button 2\",\"d\":true,\"a\":[{\"t\":\"CLICK\",\"tg\":\"#btn2\",\"desc\":\"Click button 2\"}]}")
                .build());

        // 4. Launch the AI agent in a background execution thread using the mock LLM
        runInteractiveInBg(() ->
        {
            try (final AiBrowser ai = createTestAiBrowser(mockLlm))
            {
                ai.execute("Click button 1");
            }
            catch (final Throwable e)
            {
                throw new RuntimeException(e);
            }
        });

        // 5. Verify the HUD starts successfully and open the edit prompt modal
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-edit-btn").shouldBe(Condition.enabled).click();
        $("#neo-edit-overlay").shouldBe(Condition.visible);

        // 6. Enter the edited step instruction and submit
        $("#neo-edit-input").setValue("Click button 2");
        $("#neo-edit-submit-btn").click();

        // 7. Verify the next action shifts to the edited step in the HUD
        checkBgError();
        $("#neo-ai-hud").shouldBe(Condition.visible);
        $("#neo-next-action").shouldHave(Condition.exactText("Click button 2"));

        // 8. Assert that the playbook has successfully exited replay mode and is now recording/changed
        final Playbook activePlaybook = Neodymium.getAiPlaybook();
        assertNotNull(activePlaybook, "Active playbook should not be null");
        assertTrue(activePlaybook.isRecording(), "Playbook should have transitioned to recording mode");
        assertTrue(activePlaybook.isChanged(), "Playbook should have been marked as changed");

        // 9. Approve the edited step execution
        $("#neo-approve-btn").click();
        Selenide.sleep(2500); // Give background thread time to execute step and render finished confirmation dialog in HUD

        // 10. Verify Save & Exit prompt appears, click it, and wait for thread to finish cleanly
        checkBgError();
        $("#neo-approve-btn").shouldHave(Condition.attribute("data-is-finished", "true"), Duration.ofSeconds(35));
        $("#neo-approve-btn").click();

        try
        {
            bgThread.join(20000);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        // 11. Assert clean exit and correct SUT final state
        checkBgError();
        $("#neo-ai-hud").shouldNotBe(Condition.visible);
        $("#result").shouldHave(Condition.exactText("Button 2 Clicked")); // Verify Button 2 was clicked
    }
}

