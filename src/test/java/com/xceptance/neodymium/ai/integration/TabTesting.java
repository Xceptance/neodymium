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
 * Storefront integration tests validating keyboard focus navigation using the TAB key
 * and the shift+tab key combination, leveraging the focused assertion state check.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class TabTesting extends BaseAiTest
{
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
     * Test tabbing to focus a specific target element sequentially.
     */
    @NeodymiumTest
    public final void test_TabToAnElement()
    {
        final AiExecutionResult r1 = runAi("""
                OPEN ${posters.storefront.url}
                Press the TAB key
                Press the TAB key
                Press the TAB key
                Verify that the search box has focus
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(4)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "KEY_PRESS");
        assertThat(r1).hasAction(2, "KEY_PRESS");
        assertThat(r1).hasAction(3, "KEY_PRESS");
        assertThat(r1).hasAction(4, "ASSERT");

        final String target = r1.getActions().get(4).getTarget();
        assertTrue(target.contains("search-box") || target.contains("xc_"));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi("""
                OPEN ${posters.storefront.url}
                Press the TAB key
                Press the TAB key
                Press the TAB key
                Verify that the search box has focus
                """, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);
    }

    /**
     * Test focusing an element first, then tabbing to the next focusable element.
     */
    @NeodymiumTest
    public final void test_TabAnElementToAnElement()
    {
        final AiExecutionResult r1 = runAi("""
                OPEN ${posters.storefront.url}
                Click the search box
                Verify that the search box has focus
                Press the TAB key
                Verify that the search button has focus
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(4)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");
        assertThat(r1).hasAction(2, "ASSERT");
        assertThat(r1).hasAction(3, "KEY_PRESS");
        assertThat(r1).hasAction(4, "ASSERT");

        final String targetBox = r1.getActions().get(2).getTarget();
        final String targetBtn = r1.getActions().get(4).getTarget();
        assertTrue(targetBox.contains("search-box") || targetBox.contains("xc_"));
        assertTrue(targetBtn.contains("search-button") || targetBtn.contains("xc_"));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi("""
                OPEN ${posters.storefront.url}
                Click the search box
                Verify that the search box has focus
                Press the TAB key
                Verify that the search button has focus
                """, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);
    }

    /**
     * Test pressing the TAB key multiple times specified in a single prompt step.
     */
    @NeodymiumTest
    public final void test_TabXTimes()
    {
        final AiExecutionResult r1 = runAi("""
                OPEN ${posters.storefront.url}
                Press the TAB key 3 times
                Verify that the search box has focus
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(5);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "KEY_PRESS");
        assertThat(r1).hasAction(2, "KEY_PRESS");
        assertThat(r1).hasAction(3, "KEY_PRESS");
        assertThat(r1).hasAction(4, "ASSERT");

        final String target = r1.getActions().get(4).getTarget();
        assertTrue(target.contains("search-box") || target.contains("xc_"));
    }

    /**
     * Test tabbing backwards using the Shift+Tab modifier sequence.
     */
    @NeodymiumTest
    public final void test_TabReverse()
    {
        final AiExecutionResult r1 = runAi("""
                OPEN ${posters.storefront.url}
                Press the TAB key 4 times
                Verify that the search button has focus
                Press SHIFT_TAB
                Verify that the search box has focus
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(4)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(8);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "KEY_PRESS");
        assertThat(r1).hasAction(2, "KEY_PRESS");
        assertThat(r1).hasAction(3, "KEY_PRESS");
        assertThat(r1).hasAction(4, "KEY_PRESS");
        assertThat(r1).hasAction(5, "ASSERT");
        assertThat(r1).hasAction(6, "KEY_PRESS");
        assertThat(r1).hasAction(7, "ASSERT");

        final String targetBtn = r1.getActions().get(5).getTarget();
        final String targetBox = r1.getActions().get(7).getTarget();
        assertTrue(targetBtn.contains("search-button") || targetBtn.contains("xc_"));
        assertTrue(targetBox.contains("search-box") || targetBox.contains("xc_"));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi("""
                OPEN ${posters.storefront.url}
                Press the TAB key 4 times
                Verify that the search button has focus
                Press SHIFT_TAB
                Verify that the search box has focus
                """, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(8);
    }
}
