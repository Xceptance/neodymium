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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI key press commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class KeyPressTest extends BaseAiTest
{
    private String url;
    private String extendedUrl;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        Neodymium.getData().put("keyPress.test.url", this.url);

        this.extendedUrl = String.format("http://localhost:%d/TypeActionTest/keyPressExtended.html", server.getPort());
        Neodymium.getData().put("keyPress.extended.url", this.extendedUrl);
    }

    /**
     * Test key press inputs with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testKeyPress()
    {
        final String steps = """
                OPEN ${keyPress.test.url}
                Type 'Bob' into the 'First Name:' field (hint: #first-name)
                Press the TAB key on the active element
                Type 'KeyPress works!' into the comment box
                Type 'Robert' into the 'First Name:' field
                Press the ENTER key (hint: #btn-submit)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(5)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(6)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1))
            .step(3, s -> s.isLlm(1))
            .step(4, s -> s.isLlm(1))
            .step(5, s -> s.isLlm(1));

        assertEquals("Submitted: Robert - KeyPress works!", Selenide.$("#result").text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(6)
            .hasActionsCount(6)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed())
            .step(4, s -> s.isReplayed())
            .step(5, s -> s.isReplayed());

        assertEquals("Submitted: Robert - KeyPress works!", Selenide.$("#result").text());
    }

    /**
     * Test navigation with arrow keys.
     */
    @NeodymiumTest
    public final void testArrowNavigation()
    {
        final String steps = """
                OPEN ${keyPress.extended.url}
                Click the navigation container (hint: #nav-container)
                Press ArrowDown
                Press ArrowDown
                Press ArrowUp
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(4)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(5);

        assertEquals("Active: Item 2", Selenide.$("#active-item-label").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(5)
            .hasActionsCount(5);

        assertEquals("Active: Item 2", Selenide.$("#active-item-label").text());
    }

    /**
     * Test pressing single letters individually.
     */
    @NeodymiumTest
    public final void testSingleLetterKeyPress()
    {
        final String steps = """
                OPEN ${keyPress.extended.url}
                Click the single character input field (hint: #char-input)
                Press key 'a'
                Type key 'b'
                Press 'c'
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(4)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(5);

        assertEquals("abc", Selenide.$("#char-result").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(5)
            .hasActionsCount(5);

        assertEquals("abc", Selenide.$("#char-result").text());
    }
}
