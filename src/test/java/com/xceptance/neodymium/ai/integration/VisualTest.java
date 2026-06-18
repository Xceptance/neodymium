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
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class VisualTest extends BaseAiTest
{
    // the test url
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html", server.getPort());
        Neodymium.getData().put("posters.storefront.url", this.url);
    }

    /**
     * Verifies the brown bear hero/feature photo (visual).
     */
    @NeodymiumTest
    public final void test_VisualVerification_Positive()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                A bear is shown on the right side (visual)
            """;
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasPesapCalls(1)
            .hasContextLevel(1, ContextLevel.VISUAL_LEAN)
            .step(1, s -> s.hasLlmCalls(1));

        final var s = r1.getSteps().get(1);
        assertTrue(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(s.getReasoning(), "explain that a brown bear or bear image is visible on the right side of the page");

        // no actions, LLM identified things on its own
        assertEquals(0, s.getActions().size());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true);
    }


    /**
     * Verifies that something is not there
     */
    @NeodymiumTest
    public final void test_VisualVerification_IndirectPositive()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                There is no fox visible (visual) and there is no red button either
            """;
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasPesapCalls(1)
            .hasContextLevel(1, ContextLevel.VISUAL_LEAN)
            .step(1, s -> s.hasLlmCalls(1));

        final var s = r1.getSteps().get(1);
        assertTrue(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(s.getReasoning(), "no fox and no red buttons");

        // no actions, LLM identified things on its own
        assertEquals(0, s.getActions().size());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true);
    }

    /**
     * Verifiy that things are off and avoid that we escalate if we are sure that
     * does it a clear "does not match"
     */
    @NeodymiumTest
    public final void test_VisualVerification_Negative()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                A lion dancing (visual) on the moon is shown
            """;
        final AssertionError thrown = Assertions.assertThrows(AssertionError.class, () ->
        {
            runAi(steps, VerificationMode.LIVE_LLM);
        });

        assertTrue(thrown.getMessage().contains("Verification failed: Visual verification failed:"));

        final AiExecutionResult r1 = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(r1);

        assertThat(r1)
            .hasPesapCalls(1)
            .hasContextLevel(1, ContextLevel.VISUAL_LEAN)
            .step(1, s -> s.hasLlmCalls(1));

        final var s = r1.getSteps().get(1);
        assertFalse(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(
            s.getReasoning(), 
            "There is no lion dancing on the moon.");

        // no actions, LLM identified things on its own
        assertEquals(0, s.getActions().size());

        this.resetBrowser();

        Assertions.assertThrows(AssertionError.class, () ->
        {
            runAi(steps, VerificationMode.REPLAY);
        });
    }

    /**
     * Verifies searching for 'bear' and result images context (visual).
     */
    @NeodymiumTest
    public final void test04_SearchAndVerify()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Type 'bear' into the search box.
                Click the blue button next to the input box (visual).
                Store the color of the animal shown on the second result image in the variable 'animalColor' (visual).
                """;
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(3)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "CLICK")
            .hasAction(3, "STORE", "", "animalColor");

        // Verify elements on results page
        Selenide.$("#img-bear-1").shouldBe(Condition.visible);
        Selenide.$("#img-bear-2").shouldBe(Condition.visible);
        Selenide.$("#img-bear-3").shouldBe(Condition.visible);

        final String animalColor = Neodymium.getData().asString("animalColor");
        assertEquals("green", animalColor);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "CLICK")
            .hasAction(3, "STORE", "", "animalColor")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true)
            .hasStepReplayed(3, true);

        assertEquals("green", Neodymium.getData().asString("animalColor"));
    }
}
