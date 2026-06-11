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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Storefront integration tests validating complete user keyboard-driven scenarios:
 * navigation via tabbing, inputting text, and form submission via ENTER key.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class KeyboardNavigationTesting extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");

        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html", server.getPort());
        Neodymium.getData().put("posters.storefront.url", this.url);
    }

    /**
     * Test navigating to the search box via TAB, entering a search term, 
     * and submitting the form using the ENTER key.
     */
    @NeodymiumTest
    public final void test_TabAndTypeAndSubmit()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Press the TAB key 3 times
                Verify that the search box has focus
                Type "bear"
                Press ENTER
                Verify that the search results title contains "bear"
                Verify that the page displays "3 results found."
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(7)
            .hasEscalations(1)
            .hasActionsCount(9);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "KEY_PRESS");
        assertThat(r1).hasAction(2, "KEY_PRESS");
        assertThat(r1).hasAction(3, "KEY_PRESS");
        assertThat(r1).hasAction(4, "ASSERT");
        assertThat(r1).hasAction(5, "TYPE");
        assertThat(r1).hasAction(6, "KEY_PRESS");
        assertThat(r1).hasAction(7, "ASSERT");
        assertThat(r1).hasAction(8, "ASSERT");

        final String targetBox = r1.getActions().get(5).getTarget();
        assertTrue(targetBox.contains("search-box") || targetBox.contains("xc_"));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi("""
                Open ${posters.storefront.url}
                Press the TAB key 3 times
                Verify that the search box has focus
                Type "bear"
                Press ENTER
                Verify that the search results title contains "bear"
                Verify that the page displays "3 results found."
                """, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasActionsCount(9);
    }

    /**
     * Compares keyboard navigation flow execution with and without PESAP enabled, asserting equivalent success.
     */
    @NeodymiumTest
    public final void test_TabAndTypeAndSubmit_PesapComparison()
    {
        final String steps = """
                Open ${posters.storefront.url}
                Press the TAB key 3 times
                Verify that the search box has focus
                Type "bear"
                Press ENTER
                Verify that the search results title contains "bear"
                Verify that the page displays "3 results found."
                """;

        final AiExecutionResult rWithPesap = runAi(steps, VerificationMode.LIVE_LLM, true);
        assertTrue(rWithPesap.isSuccess());

        this.resetBrowser();

        final AiExecutionResult rWithoutPesap = runAi(steps, VerificationMode.LIVE_LLM, false);
        assertTrue(rWithoutPesap.isSuccess());

        assertEquals(rWithPesap.getActions().size(), rWithoutPesap.getActions().size());
        assertEquals(rWithPesap.getActions().get(0).getType(), rWithoutPesap.getActions().get(0).getType());
        assertEquals(rWithPesap.getActions().get(4).getType(), rWithoutPesap.getActions().get(4).getType());
        assertEquals(rWithPesap.getActions().get(5).getType(), rWithoutPesap.getActions().get(5).getType());
    }
}
