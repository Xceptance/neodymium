/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.aura.interactive.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.sleep;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI integration test suite verifying AI reasoning and planned action cards rendering in the Interactive View.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("interactive-view")
@Browser("Chrome_headless")
public class InteractiveReasoningAndActionsUiTest
{
    private InteractiveConsoleServer consoleServer;
    private InteractiveConsoleEngine consoleEngine;

    @BeforeEach
    public void startStandaloneConsole() throws IOException
    {
        System.clearProperty("neodymium.managerActive");
        Configuration.browserSize = "1400x900";

        this.consoleEngine = new InteractiveConsoleEngine("test-run-" + System.currentTimeMillis());
        this.consoleServer = new InteractiveConsoleServer(this.consoleEngine);

        final String consoleUrl = "http://127.0.0.1:" + this.consoleServer.getLocalUrl().replaceAll(".*:(\\d+)$", "$1");

        Selenide.open(consoleUrl);
        sleep(800);
    }

    @AfterEach
    public void stopStandaloneConsole()
    {
        Selenide.closeWebDriver();
        if (this.consoleServer != null)
        {
            this.consoleServer.stop();
        }
    }

    @NeodymiumTest
    public void testRunningStateShowsAiThinkingBubble()
    {
        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.consoleEngine.getRunId());
        state.addProperty("status", "running");
        state.addProperty("activeStepIndex", 0);
        state.addProperty("reasoning", "AI is thinking...");

        final JsonObject blocks = new JsonObject();
        blocks.add("before", new JsonArray());

        final JsonArray steps = new JsonArray();
        final JsonObject step0 = new JsonObject();
        step0.addProperty("index", 0);
        step0.addProperty("status", "running");
        step0.addProperty("instruction", "Open login dialog");
        step0.add("actions", new JsonArray());
        steps.add(step0);

        blocks.add("steps", steps);
        blocks.add("after", new JsonArray());
        state.add("blocks", blocks);

        this.consoleEngine.broadcastSseEvent("state", state.toString());
        Selenide.executeJavaScript("if (window.applyState) window.applyState(" + state.toString() + ");");
        sleep(300);

        $(".inline-reasoning-thinking")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("AI Thinking"));
    }

    @NeodymiumTest
    public void testPausedStateDisplaysAiReasoningAndActionCards()
    {
        final String reasoningText = "I will click the Sign In button to display the authentication form.";
        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.consoleEngine.getRunId());
        state.addProperty("status", "paused");
        state.addProperty("pauseId", "pause-test-123");
        state.addProperty("activeStepIndex", 0);
        state.addProperty("reasoning", reasoningText);

        final JsonObject blocks = new JsonObject();
        blocks.add("before", new JsonArray());

        final JsonArray steps = new JsonArray();
        final JsonObject step0 = new JsonObject();
        step0.addProperty("index", 0);
        step0.addProperty("status", "running");
        step0.addProperty("instruction", "Click Sign In button");
        step0.addProperty("reasoning", reasoningText);

        final JsonArray actions = new JsonArray();
        final JsonObject act0 = new JsonObject();
        act0.addProperty("type", "click");
        act0.addProperty("target", "button#signin");
        act0.addProperty("value", "");
        act0.addProperty("description", "Click Sign In button");
        act0.addProperty("reasoning", "Target identified on current page.");
        actions.add(act0);
        step0.add("actions", actions);

        steps.add(step0);
        blocks.add("steps", steps);
        blocks.add("after", new JsonArray());
        state.add("blocks", blocks);

        this.consoleEngine.broadcastSseEvent("state", state.toString());
        Selenide.executeJavaScript("if (window.applyState) window.applyState(" + state.toString() + ");");
        sleep(300);

        $(".inline-reasoning-bubble")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text(reasoningText));

        $$(".step-card").first()
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("Click Sign In button"));
    }

    @NeodymiumTest
    public void testLlmCommunicationsRenderingInInteractiveView()
    {
        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.consoleEngine.getRunId());
        state.addProperty("status", "paused");
        state.addProperty("pauseId", "pause-llm-123");
        state.addProperty("activeStepIndex", 0);

        final JsonObject blocks = new JsonObject();
        blocks.add("before", new JsonArray());

        final JsonArray steps = new JsonArray();
        final JsonObject step0 = new JsonObject();
        step0.addProperty("index", 0);
        step0.addProperty("status", "running");
        step0.addProperty("instruction", "Search for products");

        final JsonArray llmCalls = new JsonArray();
        final JsonObject call0 = new JsonObject();
        call0.addProperty("capability", "TEXT");
        call0.addProperty("modelName", "gemini-3.6-flash");
        call0.addProperty("durationMs", 1250);
        call0.addProperty("inputTokens", 1000);
        call0.addProperty("outputTokens", 250);
        call0.addProperty("totalTokens", 1250);
        call0.addProperty("estimatedCostUsd", 0.0012);
        call0.addProperty("systemPrompt", "You are an automated web agent.");
        call0.addProperty("userPrompt", "Locate search input field.");
        call0.addProperty("responseContent", "[{\"type\":\"type\",\"target\":\"input#search\",\"value\":\"shoes\"}]");
        llmCalls.add(call0);

        step0.add("llmCalls", llmCalls);
        steps.add(step0);
        blocks.add("steps", steps);
        blocks.add("after", new JsonArray());
        state.add("blocks", blocks);

        this.consoleEngine.broadcastSseEvent("state", state.toString());
        Selenide.executeJavaScript("if (window.applyState) window.applyState(" + state.toString() + ");");
        sleep(300);

        $("#bigScreenStepDetails .llm-communications-section")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("LLM Communications (1 call)"));

        $("#bigScreenStepDetails .llm-call-card")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("Call #1: gemini-3.6-flash"));

        $("#bigScreenStepDetails .llm-subsection-card")
            .shouldBe(Condition.visible);

        $("#bigScreenStepDetails")
            .shouldHave(Condition.text("Instruction & Persona"))
            .shouldHave(Condition.text("User Query & Page Snapshot"))
            .shouldHave(Condition.text("AI Completion Payload"));

        $("#bigScreenStepDetails .llm-subsection-header").click();

        $("#bigScreenStepDetails .llm-subsection-body")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("You are an automated web agent."));
    }

    @NeodymiumTest
    public void testSubStepsTreeRenderingAndTogglingInInteractiveView()
    {
        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.consoleEngine.getRunId());
        state.addProperty("status", "running");
        state.addProperty("activeStepIndex", 0);

        final JsonObject blocks = new JsonObject();
        blocks.add("before", new JsonArray());

        final JsonArray steps = new JsonArray();
        final JsonObject parentStep = new JsonObject();
        parentStep.addProperty("index", 0);
        parentStep.addProperty("status", "running");
        parentStep.addProperty("instruction", "Search");
        parentStep.addProperty("hasSubSteps", true);

        final JsonArray subSteps = new JsonArray();

        final JsonObject sub1 = new JsonObject();
        sub1.addProperty("stepIndex", 0);
        sub1.addProperty("index", 1);
        sub1.addProperty("instruction", "Type 'Neodymium' in the Search input field");
        sub1.addProperty("status", "SUCCESS");
        sub1.addProperty("durationMs", 380L);
        subSteps.add(sub1);

        final JsonObject sub2 = new JsonObject();
        sub2.addProperty("stepIndex", 1);
        sub2.addProperty("index", 2);
        sub2.addProperty("instruction", "Click the Search");
        sub2.addProperty("status", "RUNNING");
        subSteps.add(sub2);

        parentStep.add("subSteps", subSteps);
        parentStep.add("actions", new JsonArray());
        steps.add(parentStep);

        blocks.add("steps", steps);
        blocks.add("after", new JsonArray());
        state.add("blocks", blocks);

        this.consoleEngine.broadcastSseEvent("state", state.toString());
        Selenide.executeJavaScript("if (window.applyState) window.applyState(" + state.toString() + ");");
        sleep(300);

        // Verify count badge shows completed/total
        $(".substeps-count-badge")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("1/2 Sub-steps"));

        // Verify tree container is rendered with both sub-steps
        $(".substeps-tree-container").shouldBe(Condition.visible);

        $$(".substep-row").shouldHave(CollectionCondition.size(2));

        $$(".substep-row").get(0)
            .shouldHave(Condition.text("#1.1"))
            .shouldHave(Condition.text("Type 'Neodymium' in the Search input field"))
            .shouldHave(Condition.text("0.38s"));

        $$(".substep-row").get(1)
            .shouldHave(Condition.text("#1.2"))
            .shouldHave(Condition.text("Click the Search"));

        // Toggle tree collapse
        $(".substeps-count-badge").click();
        $(".substeps-tree-container").shouldNotBe(Condition.exist);

        // Toggle tree expand
        $(".substeps-count-badge").click();
        $(".substeps-tree-container").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public void testSubStepInspectorScoping()
    {
        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.consoleEngine.getRunId());
        state.addProperty("status", "paused");
        state.addProperty("pauseId", "pause-substep-123");
        state.addProperty("activeStepIndex", 0);

        final JsonObject blocks = new JsonObject();
        blocks.add("before", new JsonArray());

        final JsonArray steps = new JsonArray();
        final JsonObject parentStep = new JsonObject();
        parentStep.addProperty("index", 0);
        parentStep.addProperty("status", "running");
        parentStep.addProperty("instruction", "Search");
        parentStep.addProperty("hasSubSteps", true);

        final JsonArray actions = new JsonArray();
        final JsonObject act1 = new JsonObject();
        act1.addProperty("type", "TYPE");
        act1.addProperty("target", "#searchInput");
        act1.addProperty("value", "Neodymium");
        act1.addProperty("subStepIndex", 0);
        actions.add(act1);
        parentStep.add("actions", actions);

        final JsonArray subSteps = new JsonArray();

        final JsonObject sub1 = new JsonObject();
        sub1.addProperty("stepIndex", 0);
        sub1.addProperty("index", 1);
        sub1.addProperty("instruction", "Type 'Neodymium' in the Search input field");
        sub1.addProperty("status", "SUCCESS");
        sub1.addProperty("durationMs", 380L);
        sub1.addProperty("file", "fragments/search.yaml");
        sub1.addProperty("line", 8);
        subSteps.add(sub1);

        final JsonObject sub2 = new JsonObject();
        sub2.addProperty("stepIndex", 1);
        sub2.addProperty("index", 2);
        sub2.addProperty("instruction", "Click the Search");
        sub2.addProperty("status", "RUNNING");
        sub2.addProperty("file", "fragments/search.yaml");
        sub2.addProperty("line", 9);
        subSteps.add(sub2);

        parentStep.add("subSteps", subSteps);
        steps.add(parentStep);

        blocks.add("steps", steps);
        blocks.add("after", new JsonArray());
        state.add("blocks", blocks);

        this.consoleEngine.broadcastSseEvent("state", state.toString());
        Selenide.executeJavaScript("if (window.applyState) window.applyState(" + state.toString() + ");");
        sleep(300);

        // Verify tabs bar is rendered in Step Details panel
        $("#bigScreenStepDetails .substep-tabs-bar").shouldBe(Condition.visible);
        $$("#bigScreenStepDetails .substep-tab-btn").shouldHave(CollectionCondition.size(3));

        // Click sub-step 1 in the timeline to scope details
        $$(".substep-row").get(0).click();

        $("#bigScreenStepDetails")
            .shouldHave(Condition.text("Sub-step #1.1: Type 'Neodymium' in the Search input field"))
            .shouldHave(Condition.text("fragments/search.yaml:8"))
            .shouldHave(Condition.text("TYPE"))
            .shouldHave(Condition.text("#searchInput"))
            .shouldHave(Condition.text("Neodymium"));

        // Click tab #1.2
        $$("#bigScreenStepDetails .substep-tab-btn").get(2).click();

        $("#bigScreenStepDetails")
            .shouldHave(Condition.text("Sub-step #1.2: Click the Search"))
            .shouldHave(Condition.text("fragments/search.yaml:9"));

        // Click Overview tab
        $$("#bigScreenStepDetails .substep-tab-btn").get(0).click();

        $("#bigScreenStepDetails")
            .shouldHave(Condition.text("Step #1: Search"));
    }
}

