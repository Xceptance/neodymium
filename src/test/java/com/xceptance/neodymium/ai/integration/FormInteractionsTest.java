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
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies interactive forms entry, client-side input validations,
 * reactive pricing calculations, and synchronous overlap visual assertions.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1024x768")
@Tag("form")
@Tag("llm")
public final class FormInteractionsTest extends BaseAiTest
{
    private String formsUrl;

    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.formsUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/forms.html", server.getPort());
        Neodymium.getData().put("form.test.url", this.formsUrl);
    }

    @NeodymiumTest
    public void testFormInteractionsAndVerifications()
    {
        final String steps = """
                OPEN ${form.test.url}
                Type 'Jane Smith' into the 'Full Name' field.
                Type 'invalidemail' into the 'Email Address' field.
                Click the 'Create Account' button.
                Verify that the email format invalid warning 'This email format is invalid.' is visible.
                Type '2' into the 'Neon Gradient Poster' quantity field. (hint: #qty-poster-1)
                Verify that the Total Price is '$39.98'.
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(6)
            .hasPesapCalls(6)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(7)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("CLICK")))
            .step(4, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")))
            .step(5, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(6, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasPesapCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(7)
            .hasActionsCount(7)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("CLICK")))
            .step(4, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")))
            .step(5, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(6, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")));
    }

    @NeodymiumTest
    public void testFormTypeOverwritesNotAppends()
    {
        final String steps = """
                OPEN ${form.test.url}
                Type 'Jane' into the 'Full Name' field.
                Type 'Smith' into the 'Full Name' field.
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")));

        assertEquals("Smith", com.codeborne.selenide.Selenide.$("#reg-username").val());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasPesapCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")));

        assertEquals("Smith", com.codeborne.selenide.Selenide.$("#reg-username").val());
    }

    @NeodymiumTest
    public void testReplayWithDismissedBanner()
    {
        final String steps = """
                OPEN ${form.test.url}
                Type 'Jane' into the 'Full Name' field.
                Click the 'Close Banner' button (hint: #promo-close-btn)
                Type 'jane@doe.com' into the 'Email Address' field.
                Click the 'Create Account' button.
                Verify that the success message 'Account Registered!' is visible.
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(5)
            .hasPesapCalls(5)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(6)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("CLICK")))
            .step(3, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(4, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("CLICK")))
            .step(5, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasPesapCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(6)
            .hasActionsCount(6)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("CLICK")))
            .step(3, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(4, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("CLICK")))
            .step(5, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());
    }

    @NeodymiumTest
    public void testFormNumericInputHandling()
    {
        final String steps = """
                OPEN ${form.test.url}
                Type '5' into the 'Neon Gradient Poster' quantity field. (hint: #qty-poster-1)
                Verify that the Total Price is '$99.95'.
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")));

        assertEquals("$99.95", com.codeborne.selenide.Selenide.$("#cart-total").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasPesapCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("ASSERT")));

        assertEquals("$99.95", com.codeborne.selenide.Selenide.$("#cart-total").text());
    }

    @NeodymiumTest
    public void testFormCombinedInstructionsUpfront()
    {
        final String steps = """
                OPEN ${form.test.url}
                Type 'Jane Smith' into the 'Full Name' field, enter 'jane.smith@example.com' into the 'Email Address' field, then click the 'Create Account' button
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasExpandedInstruction("Type 'Jane Smith' into the 'Full Name' field")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasExpandedInstruction("enter 'jane.smith@example.com' into the 'Email Address' field")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.hasExpandedInstruction("click the 'Create Account' button")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("CLICK")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("CLICK")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());
    }

    @NeodymiumTest
    public void testFormCombinedInstructionsUpfrontGerman()
    {
        final String steps = """
                OPEN ${form.test.url}
                Trage 'Jane Smith' in das Feld 'Full Name' ein, gib 'jane.smith@example.com' in das Feld 'Email Address' ein und klicke auf 'Create Account'
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasExpandedInstruction("Trage 'Jane Smith' in das Feld 'Full Name' ein")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasExpandedInstruction("gib 'jane.smith@example.com' in das Feld 'Email Address' ein")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.hasExpandedInstruction("klicke auf 'Create Account'")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("CLICK")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("CLICK")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());
    }

    @NeodymiumTest
    public void testFormCombinedInstructionsUpfrontFrench()
    {
        final String steps = """
                OPEN ${form.test.url}
                Saisir 'Jane Smith' dans le champ 'Full Name', entrer 'jane.smith@example.com' dans le champ 'Email Address', puis cliquer sur le bouton 'Create Account'
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse().action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.hasExpandedInstruction("Saisir 'Jane Smith' dans le champ 'Full Name'")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.hasExpandedInstruction("entrer 'jane.smith@example.com' dans le champ 'Email Address'")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.hasExpandedInstruction("cliquer sur le bouton 'Create Account'")
                           .hasLlmCalls(1).hasPesapCall().hasActionsCount(1).action(0, a -> a.hasType("CLICK")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("NAVIGATE")))
            .step(1, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(2, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("TYPE")))
            .step(3, s -> s.isReplayed().hasActionsCount(1).action(0, a -> a.hasType("CLICK")));

        assertEquals("Account Registered!", com.codeborne.selenide.Selenide.$("#reg-status-success").text());
    }
}
