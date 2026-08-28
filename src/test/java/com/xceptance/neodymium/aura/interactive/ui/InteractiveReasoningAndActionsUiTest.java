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
import org.neodymium.ai.testing.BaseAiTest;

import com.codeborne.selenide.Condition;
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
public class InteractiveReasoningAndActionsUiTest extends BaseAiTest
{
    private InteractiveConsoleServer consoleServer;
    private InteractiveConsoleEngine consoleEngine;

    @BeforeEach
    public void startStandaloneConsole() throws IOException
    {
        System.clearProperty("neodymium.managerActive");

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

        $(".inline-reasoning-bubble")
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text(reasoningText));

        $$(".step-card").first()
            .shouldBe(Condition.visible)
            .shouldHave(Condition.text("Click Sign In button"));
    }
}
