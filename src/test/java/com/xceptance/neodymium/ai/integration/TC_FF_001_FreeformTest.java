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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.ai.integration;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.testing.LlmAssert;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * A freeform test case migrated to neodymium-library to test storefront functionalities
 * like searching, product details, and image content validation.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class TC_FF_001_FreeformTest extends BaseAiTest
{
    // the test url
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        // we don't need that, we start with AXTREE as default
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");

        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html", server.getPort());
        Neodymium.getData().put("posters.storefront.url", this.url);
    }

    /**
     * Verifies the brown bear hero/feature photo (visual).
     */
    @NeodymiumTest
    public final void test_VisualVerification_Positive()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                A bear is shown on the right side (visual)
            """, VerificationMode.LIVE_LLM);

        final var s = r1.getSteps().get(1);
        assertEquals(1, s.getLlmCalls().size());
        assertEquals(ContextLevel.VISUAL_LEAN, s.getLlmCalls().get(0).getContextLevel());

        assertTrue(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(s.getReasoning(), "explain that a brown bear or bear image is visible on the right side of the page");

        // no actions, LLM identified things on its own
        final var a = s.getActions();
        assertEquals(0, a.size());
    }

    /**
     * Verifiy that things are off
     */
    @NeodymiumTest
    public final void test_VisualVerification_Negative()
    {
        final AssertionError thrown = Assertions.assertThrows(AssertionError.class, () ->
        {
            runAi("""
                    Open ${posters.storefront.url}
                    A lion dancing (visual) on the moon is shown
                """, VerificationMode.LIVE_LLM);
        });

        assertTrue(thrown.getMessage().contains("Verification failed: Visual verification failed:"));

        final AiExecutionResult r1 = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(r1);

        final var s = r1.getSteps().get(1);
        assertEquals(1, s.getLlmCalls().size());
        assertEquals(ContextLevel.VISUAL_LEAN, s.getLlmCalls().get(0).getContextLevel());

        assertFalse(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(
            s.getReasoning(), 
            "There is not lion dancing on the moon.");

        // no actions, LLM identified things on its own
        final var a = s.getActions();
        assertEquals(0, a.size());
    }

    /**
     * Verifies base styling and logo element (visual).
     */
    @NeodymiumTest
    public final void test03_ColorsAndLogo()
    {
        final AiExecutionResult r1 = runAi("Open ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");

        Selenide.$(".brand .brand-icon").shouldHave(Condition.text("✕"));
        Selenide.$(".brand").shouldHave(Condition.text("Posters"));
        
        final String bodyBg = Selenide.$("body").getCssValue("background-color");
        assertTrue(bodyBg.contains("255, 255, 255") || bodyBg.equals("rgb(255, 255, 255)"));
    }

    /**
     * Verifies searching for 'bear' and result images context (visual).
     */
    @NeodymiumTest
    public final void test04_SearchAndVerify()
    {
        final AiExecutionResult r = runAi("""
                Open ${posters.storefront.url}
                Type 'bear' into the search box.
                Click the blue button next to the input box (visual).
                Store the color of the animal shown on the second result image in the variable 'animalColor' (visual).
                """, VerificationMode.LIVE_LLM);

        assertThat(r)
            .hasLlmCalls(3)
            .hasNoEscalations()
            .hasActionsCount(4);

        assertThat(r).hasAction(0, "NAVIGATE");
        assertThat(r).hasAction(1, "TYPE", "xc_", "bear");
        assertThat(r).hasAction(2, "CLICK");
        assertThat(r).hasAction(3, "STORE", "", "animalColor");

        // Verify elements on results page
        Selenide.$("#img-bear-1").shouldBe(Condition.visible);
        Selenide.$("#img-bear-2").shouldBe(Condition.visible);
        Selenide.$("#img-bear-3").shouldBe(Condition.visible);

        final String animalColor = Neodymium.getData().asString("animalColor");
        assertEquals("green", animalColor);
    }

    /**
     * Verifies searching for 'bear' explicitly step-by-step, validating the execution stats and browser state directly.
     */
    @NeodymiumTest
    public final void test05_ExplicitSearchAndVerify()
    {
        // Step 1: Open SUT homepage, no LLM, direct parser match due to OPEN
        final AiExecutionResult r1 = runAi("Open ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");

        // we are now in posters
        assertEquals("Posters Art Store", Selenide.title());

        // Step 2: Type 'bear' into the search input box
        final AiExecutionResult r2 = runAi("Type 'bear' into the search box.", VerificationMode.LIVE_LLM);
        Selenide.$("#search-box").shouldHave(Condition.value("bear"));
        
        assertThat(r2)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasContextLevel(ContextLevel.AXTREE)
            .hasAction(0, "TYPE", "xc_", "bear");

        // Step 3: Click the search submit button
        final AiExecutionResult r3 = runAi("Click the blue button next to the input box (visual).", VerificationMode.LIVE_LLM);
        assertTrue(Selenide.title().contains("Search Results"));
        
        assertThat(r3)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasAction(0, "CLICK");
        
        final String target3 = r3.getActions().get(0).getTarget();
        assertTrue(target3.contains("search-button") || target3.contains("submit") || target3.contains("xc_"), "Target mismatch for Step 3");

        // Step 4: Perform visual extraction of the animal color and verify elements directly
        final AiExecutionResult r4 = runAi("Store the color of the animal shown on the second result image in the variable 'animalColor' (visual).", VerificationMode.LIVE_LLM);
        
        assertThat(r4)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasAction(0, "STORE", "", "animalColor");

        final String target4 = r4.getActions().get(0).getTarget();
        assertTrue(target4 == null || target4.isEmpty() || target4.contains("gummy_bears.png") || target4.contains("img-bear-2") || target4.contains("result") || target4.contains("xc_"), "Target mismatch for Step 4");

        // Direct assertions on the target search result image elements
        Selenide.$("#img-bear-1").shouldBe(Condition.visible);
        Selenide.$("#img-bear-2").shouldBe(Condition.visible);
        Selenide.$("#img-bear-3").shouldBe(Condition.visible);

        // Verify the extracted visual variable value in Java
        final String animalColor = Neodymium.getData().asString("animalColor");
        assertEquals("green", animalColor);
    }
    
    /**
     * We do not deal with incorrect spelling or other languages easily, so we ask the LLM
     * instead.
     */
    @NeodymiumTest
    public final void test06_ParserDoesNotWorkWithGerman()
    {
        // we don't need that, we start with AXTREE as default
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");

        // Step 1: Open SUT homepage, no LLM, no parser match due to Gehe zu
        final AiExecutionResult r1 = runAi("Gehe zu ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(1)
            .hasContextLevel(ContextLevel.AXTREE)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");

        // we are now in posters
        assertEquals("Posters Art Store", Selenide.title());
    }

    /**
     * Test a multi-step instruction with step-by-step verification of status.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void test_MultiStepExecution()
    {
        final AiExecutionResult r1 = runAi("Open ${posters.storefront.url}\nrefresh", VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(2)
            .hasStepDirectParsed(0, true)
            .hasStepDirectParsed(1, true)
            .hasStepReplayed(0, false)
            .hasStepReplayed(1, false);

        assertEquals("Posters Art Store", Selenide.title());

        // close it and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi("Open ${posters.storefront.url}\nrefresh", VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .hasStepDirectParsed(0, false)
            .hasStepDirectParsed(1, false)
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true);

        assertEquals("Posters Art Store", Selenide.title());
    }
}
