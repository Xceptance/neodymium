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
package com.xceptance.neodymium.aura.interactive.unit;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Selenide UI test verifying interactive console error recovery controls:
 * <ul>
 *   <li>Error Mode button transformation (Heal button, orange Finish button)</li>
 *   <li>Transition back to Normal Mode on step edit</li>
 *   <li>"Suggest Fix" button on failed step cards and Suggestion Overlay workflow</li>
 *   <li>Failure icon on final save overlay when test finishes with failure</li>
 * </ul>
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public final class InteractiveViewErrorRecoveryAndSuggestFixTest extends BaseInteractiveViewTest
{
    private String buildStateWithFailedStep(final String pauseId)
    {
        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.engine.getRunId());
        state.addProperty("status", "paused");
        state.addProperty("pauseId", pauseId);
        state.addProperty("currentStepIndex", 0);
        state.addProperty("testName", "Error Recovery Test");

        final JsonObject blocks = new JsonObject();
        final JsonArray steps = new JsonArray();

        final JsonObject step0 = new JsonObject();
        step0.addProperty("index", 0);
        step0.addProperty("instruction", "Click missing search icon '.search-trigger'");
        step0.addProperty("status", "failed");
        step0.addProperty("failed", true);
        step0.addProperty("errorMessage", "Element not found: .search-trigger");
        steps.add(step0);

        final JsonObject step1 = new JsonObject();
        step1.addProperty("index", 1);
        step1.addProperty("instruction", "Type query 'Neodymium'");
        step1.addProperty("status", "pending");
        steps.add(step1);

        blocks.add("before", new JsonArray());
        blocks.add("steps", steps);
        blocks.add("after", new JsonArray());
        state.add("blocks", blocks);

        return state.toString();
    }

    @Test
    public final void testErrorModeButtonsAndTransitionOnStepEdit() throws Exception
    {
        Configuration.clickViaJs = true;

        final String pauseId = "pause-error-1";
        this.engine.registerPauseId(pauseId);
        this.engine.pushState(buildStateWithFailedStep(pauseId));

        openConsoleUrl();

        // 1. Verify Error Mode on Toolbar
        final SelenideElement btnRun = $("#btnRun");
        btnRun.should(Condition.exist);
        btnRun.shouldHave(Condition.cssClass("btn-heal"));
        btnRun.shouldHave(Condition.text("Heal"));

        final SelenideElement btnFinish = $("#btnFinish");
        btnFinish.should(Condition.exist);
        btnFinish.shouldBe(Condition.visible);

        // 2. Verify "Suggest Fix" button on failed step card
        final SelenideElement step0Card = $(".step-card[data-step-idx='0']");
        step0Card.should(Condition.exist);
        step0Card.shouldHave(Condition.cssClass("failed"));

        final SelenideElement btnSuggestFix = step0Card.$(".btn-suggest-fix");
        btnSuggestFix.should(Condition.exist);
        btnSuggestFix.shouldBe(Condition.visible);

        // 3. Edit Step 0 to trigger transition back to Normal Mode
        final SelenideElement editBtn = step0Card.$(".step-edit-btn");
        editBtn.should(Condition.exist);
        editBtn.click();

        final SelenideElement textarea = step0Card.$(".inline-edit-textarea");
        textarea.should(Condition.exist);
        textarea.clear();
        textarea.sendKeys("Click updated search button '#searchBtn'");

        final SelenideElement saveBtn = step0Card.$(".step-save-btn");
        saveBtn.click();

        // 4. Verify transition back to Normal Mode
        btnRun.shouldHave(Condition.cssClass("btn-success"));
        btnRun.shouldHave(Condition.text("Run"));
        btnFinish.shouldNotBe(Condition.visible);
    }

    @Test
    public final void testSuggestFixModalWorkflow() throws Exception
    {
        Configuration.clickViaJs = true;

        final String pauseId = "pause-error-2";
        this.engine.registerPauseId(pauseId);
        this.engine.pushState(buildStateWithFailedStep(pauseId));

        openConsoleUrl();

        // 1. Click "Suggest Fix" button on failed step card
        final SelenideElement step0Card = $(".step-card[data-step-idx='0']");
        step0Card.should(Condition.exist);

        final SelenideElement btnSuggestFix = step0Card.$(".btn-suggest-fix");
        btnSuggestFix.should(Condition.exist);
        btnSuggestFix.click();

        // 2. Verify Suggestion Overlay opens in loading state
        final SelenideElement overlay = $("#suggestionOverlay");
        overlay.should(Condition.exist);
        overlay.shouldHave(Condition.cssClass("active"));
        $("#suggestionLoading").shouldBe(Condition.visible);

        // 3. Simulate backend fixSuggestion SSE broadcast / handler invocation
        final String fixEventJson = "{\"originalInstruction\":\"Click missing search icon '.search-trigger'\",\"suggestedInstruction\":\"Click header search button 'button[aria-label=\\\"Search\\\"]'\"}";
        this.engine.broadcastSseEvent("fixSuggestion", fixEventJson);
        com.codeborne.selenide.Selenide.executeJavaScript("handleFixSuggestionReceived(" + fixEventJson + ");");

        // 4. Verify original text and editable suggestion in popup
        $("#suggestionBody").shouldBe(Condition.visible);
        final SelenideElement origText = $("#suggestionOriginalText");
        origText.should(Condition.exist);
        origText.shouldHave(Condition.text("Click missing search icon '.search-trigger'"));

        final SelenideElement textarea = $("#suggestionTextarea");
        textarea.should(Condition.exist);
        textarea.shouldHave(Condition.value("Click header search button 'button[aria-label=\"Search\"]'"));

        // 5. Modify suggestion in popup and click Apply Only
        textarea.clear();
        textarea.sendKeys("Click main search button '#mainSearch'");

        final SelenideElement btnApply = $("#suggestionActions button.btn-primary");
        btnApply.click();

        // 6. Verify overlay closes
        overlay.shouldNotHave(Condition.cssClass("active"));
    }

    @Test
    public final void testFinishOverlayDisplaysFailureIconOnFailedRun() throws Exception
    {
        Configuration.clickViaJs = true;

        final String pauseId = "pause-final-999";
        this.engine.registerPauseId(pauseId);

        final JsonObject state = new JsonObject();
        state.addProperty("runId", this.engine.getRunId());
        state.addProperty("status", "failed");
        state.addProperty("pauseId", pauseId);
        state.addProperty("testName", "Failed Test Run");

        this.engine.pushState(state.toString());

        openConsoleUrl();

        // Verify final save overlay renders failure heading with red icon
        final SelenideElement finalOverlay = $("#finalSaveOverlay");
        finalOverlay.should(Condition.exist);
        finalOverlay.shouldHave(Condition.cssClass("active"));

        final SelenideElement finalTitle = $("#finalSaveTitle");
        finalTitle.should(Condition.exist);
        finalTitle.shouldHave(Condition.cssClass("failed"));
        finalTitle.$(".material-symbols-outlined").shouldHave(Condition.exactText("cancel"));
    }
}
