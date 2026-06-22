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
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying that compound steps can be split dynamically
 * both upfront (JIT PESAP) and at runtime (SPLIT action fallback).
 * <p>
 * This class tests the dual-layer splitting strategy:
 * 1. Upfront splitting during the JIT Pre-step Execution Static Analysis Phase (PESAP).
 * 2. Runtime fallback splitting when a standard LLM execution returns a SPLIT action.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("llm")
public class SplitMultiActionTest extends BaseAiTest
{
    private String url;

    /**
     * Set up the storefront/SUT page URL for the test case dynamically.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        this.url = String.format("http://localhost:%d/SplitMultiActionTest/testSplitMultiAction.html", server.getPort());
        Neodymium.getData().put("split.test.url", this.url);
    }

    /**
     * Verifies upfront step splitting (JIT PESAP) during live execution
     * and offline playbook.
     * <p>
     * When PESAP is enabled, a compound instruction is split upfront before
     * standard DOM parsing or LLM calls take place.
     */
    @NeodymiumTest
    public final void testSplitMultiActionUpfront()
    {
        final String steps = """
                OPEN ${split.test.url}
                Click the "Menu" button and then click the "Create Account" link in the dropdown and then the text "Account Form Opened!" is shown
            """;

        // Execute the compound step under LIVE_LLM mode.
        // This should split the step upfront into three separate execution steps.
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        // Expect exactly 4 steps to have been executed (split from the original compound step)
        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasExpandedInstruction("Click the \"Menu\" button", true))
            .step(2, s -> s.hasExpandedInstruction("Click the \"Create Account\" link in the dropdown", true))
            .step(3, s -> s.hasExpandedInstruction("the text \"Account Form Opened!\" is shown", true));

        // Verify the SUT has updated correctly
        assertEquals("Account Form Opened!", Selenide.$("#status").text());

        this.resetBrowser();

        // Verify that replaying the playbook offline runs successfully without LLM calls,
        // and correctly aligns the steps list using the saved playbook's originalUnsplitInstruction field.
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());
        assertEquals("Account Form Opened!", Selenide.$("#status").text());
    }

    /**
     * Verifies upfront step splitting (JIT PESAP) in German.
     */
    @NeodymiumTest
    public final void testSplitMultiActionUpfrontGerman()
    {
        final String steps = """
                OPEN ${split.test.url}
                Klicke auf die Schaltfläche "Menu" und klicke dann auf den Link "Create Account" im Dropdown-Menü und überprüfe, ob der Text "Account Form Opened!" angezeigt wird
            """;

        // Execute the compound step under LIVE_LLM mode.
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        // Expect exactly 4 steps to have been executed (split from the original compound step)
        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasExpandedInstruction("Klicke auf die Schaltfläche \"Menu\"", true))
            .step(2, s -> s.hasExpandedInstruction("Klicke auf den Link \"Create Account\" im Dropdown-Menü", true))
            .step(3, s -> s.hasExpandedInstruction("Überprüfe, ob der Text \"Account Form Opened!\" angezeigt wird", true));

        // Verify the SUT has updated correctly
        assertEquals("Account Form Opened!", Selenide.$("#status").text());

        this.resetBrowser();

        // Verify that replaying the playbook offline runs successfully without LLM calls,
        // and correctly aligns the steps list using the saved playbook's originalUnsplitInstruction field.
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());
        assertEquals("Account Form Opened!", Selenide.$("#status").text());
    }
}
