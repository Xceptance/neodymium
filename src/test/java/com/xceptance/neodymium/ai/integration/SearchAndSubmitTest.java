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
// AI-generated: Claude Opus 4.6
package com.xceptance.neodymium.ai.integration;

import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the LLM's ability to decompose "search and submit" instructions
 * into the correct action sequence (TYPE + KEY_PRESS ENTER or TYPE + CLICK)
 * without explicit prompt guidance. Tests both English and German natural
 * language instructions against the Posters storefront search form.
 *
 * @author AI-generated: Claude Opus 4.6
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public final class SearchAndSubmitTest extends BaseAiTest
{
    private String storefrontUrl;

    /**
     * Set up storefront URL before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.storefrontUrl = String.format(
            "http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html",
            server.getPort());
        Neodymium.getData().put("posters.storefront.url", this.storefrontUrl);
    }

    /**
     * English: type a search term into the search box and submit the search,
     * then verify the results page shows the expected content.
     * The LLM must decompose "search and submit" into TYPE + submission action.
     */
    @NeodymiumTest
    public final void testSearchAndSubmitEnglish()
    {
        final String steps = """
                Open ${posters.storefront.url}
                Type 'bear' into the search box and submit the search
                Verify that the search results title contains 'bear'
                Verify that the page displays '3 results found.'
                """;
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertTrue(r1.isSuccess(), "Search and submit flow should succeed");

        // 4 LLM calls: TYPE+submit, ASSERT(title), ASSERT(count @ AXTREE escalate), ASSERT(count @ STANDARD)
        assertThat(r1)
            .hasLlmCalls(3)
            .hasPesapCalls(3)
            .hasEscalations(0)
            .hasDirectParses(1)
            .hasActionsCount(5);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "TYPE");
        // Action 2: CLICK (Search button) or KEY_PRESS (Enter) — both are valid submissions
        assertThat(r1).hasAction(3, "ASSERT");
        assertThat(r1).hasAction(4, "ASSERT");

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);
    }

    /**
     * German: Einen Suchbegriff in das Suchfeld eingeben und die Suche absenden,
     * dann die Ergebnisseite auf den erwarteten Inhalt überprüfen.
     * Identical structural expectations regardless of instruction language.
     */
    @NeodymiumTest
    public final void testSearchAndSubmitGerman()
    {
        final String steps = """
                Beginne mit der URL ${posters.storefront.url}
                Gib 'bear' in das Suchfeld ein und sende die Suche ab
                Überprüfe, dass der Titel der Suchergebnisse 'bear' enthält
                Überprüfe, dass auf der Seite '3 results found.' angezeigt wird
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertTrue(r1.isSuccess(), "German search and submit flow should succeed");

        assertThat(r1)
            .hasLlmCalls(4)
            .hasPesapCalls(4)
            .hasEscalations(0)
            .hasDirectParses(0) // Beginne is not a direct command
            .hasActionsCount(5);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "TYPE");
        assertThat(r1).hasAction(3, "ASSERT");
        assertThat(r1).hasAction(4, "ASSERT");

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);
    }
}
