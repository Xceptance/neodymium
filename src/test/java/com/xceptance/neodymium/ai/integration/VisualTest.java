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

        assertThat(r1).hasNoPesapCalls();

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
     * Verifies that something is not there
     */
    @NeodymiumTest
    public final void test_VisualVerification_IndirectPositive()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                There is no fox visible (visual) and there is no red button either
            """, VerificationMode.LIVE_LLM);

        assertThat(r1).hasNoPesapCalls();

        final var s = r1.getSteps().get(1);
        assertEquals(1, s.getLlmCalls().size());
        assertEquals(ContextLevel.VISUAL_LEAN, s.getLlmCalls().get(0).getContextLevel());

        assertTrue(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(s.getReasoning(), "no fox and no red buttons");

        // no actions, LLM identified things on its own
        final var a = s.getActions();
        assertEquals(0, a.size());
    }

    /**
     * Verifiy that things are off and avoid that we escalate if we are sure that
     * does it a clear "does not match"
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

        assertThat(r1).hasNoPesapCalls();

        final var s = r1.getSteps().get(1);
        assertEquals(1, s.getLlmCalls().size());
        assertEquals(ContextLevel.VISUAL_LEAN, s.getLlmCalls().get(0).getContextLevel());

        assertFalse(s.getLlmCalls().get(0).isSuccess());
        assertTrue(s.getLlmCalls().get(0).isDone());

        // Extract and verify LLM reasoning semantically
        LlmAssert.assertViaLlmSemanticMatch(
            s.getReasoning(), 
            "There is no lion dancing on the moon.");

        // no actions, LLM identified things on its own
        final var a = s.getActions();
        assertEquals(0, a.size());
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
            .hasPesapCalls(1)
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
}
