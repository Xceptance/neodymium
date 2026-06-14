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
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI type commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class TypeTest extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        Neodymium.getData().put("type.test.url", this.url);
    }

    /**
     * Test input field typing with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testType()
    {
        final String steps = """
                OPEN ${type.test.url}
                Type 'Bob' into the 'First Name:' field (hint: #first-name)
                Type 'Hello AI' into the 'Comments:' textarea (hint: #comments)
                Click the 'Submit Details' button (hint: #btn-submit)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(3)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1))
            .step(3, s -> s.isLlm(1));

        assertEquals("Submitted: Bob - Hello AI", Selenide.$("#result").text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(4)
            .hasActionsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertEquals("Submitted: Bob - Hello AI", Selenide.$("#result").text());
    }
}
