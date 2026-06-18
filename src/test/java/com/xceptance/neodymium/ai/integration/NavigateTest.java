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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
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
public class NavigateTest extends BaseAiTest
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
     * Test a regular open without PESAP. The open is recognized by the ACTION framework and
     * we are not calling the LLM
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void test_Open()
    {
        final AiExecutionResult r1 = runAi("OPEN ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasDirectParses(1)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");
        assertEquals(this.url, r1.getActions().get(0).getValue());

        assertEquals("Posters Art Store", Selenide.title());

        // close it and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi("OPEN ${posters.storefront.url}", VerificationMode.REPLAY);
        
        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasDirectParses(0)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");
        assertEquals(this.url, r2.getActions().get(0).getValue());

        assertEquals("Posters Art Store", Selenide.title());
    }

    /**
     * We open in LLM mode and later we want to replay but with a change
     * so the replay is not using the replay.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void test_OpenWithPlaybookChange()
    {
        final AiExecutionResult r1 = runAi("OPEN ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");
        assertEquals(this.url, r1.getActions().get(0).getValue());
        assertEquals("Posters Art Store", Selenide.title());

        // close it and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi("Let's open ${posters.storefront.url}", VerificationMode.REPLAY);
        
        assertThat(r2)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(0)
            .hasActionsCount(1)
            .hasContextLevel(0, ContextLevel.AXTREE)
            .hasAction(0, "NAVIGATE");
        assertEquals(this.url, r2.getActions().get(0).getValue());

        assertEquals("Posters Art Store", Selenide.title());
    }

    /**
     * Test an open we don't know aka have to ask the LLM
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void test_Open_WithLLM()
    {
        final AiExecutionResult r1 = runAi("Get to ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasDirectParses(0)
            .hasContextLevel(0, ContextLevel.AXTREE)
            .hasAction(0, "NAVIGATE");
        assertEquals(this.url, r1.getActions().get(0).getValue());
        assertEquals("Posters Art Store", Selenide.title());
    }

    /**
     * We do not deal with incorrect spelling or other languages easily, so we ask the LLM
     * instead.
     */
    @NeodymiumTest
    public final void test_Open_ParserDoesNotWorkWithGerman()
    {
 
        // Step 1: Open SUT homepage, no LLM, no parser match due to Gehe zu
        final AiExecutionResult r1 = runAi("Gehe zu ${posters.storefront.url}", VerificationMode.LIVE_LLM);
        
        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasContextLevel(0, ContextLevel.AXTREE)
            .hasNoEscalations()
            .hasActionsCount(1)
            .hasAction(0, "NAVIGATE");


        // we are now in posters
        assertEquals("Posters Art Store", Selenide.title());
    }
}
