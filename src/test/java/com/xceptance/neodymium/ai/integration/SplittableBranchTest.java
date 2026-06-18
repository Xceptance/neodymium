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
import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test challenging both step splitting (JIT PESAP) and conditional branches
 * simultaneously within a single compound instruction.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class SplittableBranchTest extends BaseAiTest
{
    private String url;

    /**
     * Set up SUT URL dynamically.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        this.url = String.format("http://localhost:%d/SplittableBranchTest/testSplittableBranch.html", server.getPort());
        Neodymium.getData().put("splittable.branch.url", this.url);
    }

    /**
     * Verifies that a compound instruction containing a conditional branch (If... then... else...)
     * is correctly split upfront (preserving the branch intact) and successfully executed.
     */
    @NeodymiumTest
    public final void testSplittableBranchHappyPath()
    {
        final String steps = """
                OPEN ${splittable.branch.url}
                Click the Menu button, and then if the Accept Cookies button is visible, then click the Accept Cookies button, else click the Main Action Button, and then verify the text 'Cookies Accepted!' is shown
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasExpandedInstruction("Click the Menu button", true))
            .step(2, s -> s.hasExpandedInstruction("If the Accept Cookies button is visible, then click the Accept Cookies button, else click the Main Action Button", true))
            .step(3, s -> s.hasExpandedInstruction("verify the text 'Cookies Accepted!' is shown", true));

        assertEquals("Cookies Accepted!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertEquals("Cookies Accepted!", Selenide.$("#result").text());
    }

    /**
     * Verifies that the branch alternative (the 'else' action) works correctly within
     * the split compound instruction when the condition is false.
     */
    @NeodymiumTest
    public final void testSplittableBranchElsePath()
    {
        final String steps = """
                OPEN ${splittable.branch.url}?noCookies=true
                Click the Menu button, and then if the Accept Cookies button is visible, then click the Accept Cookies button, else click the Main Action Button, and then verify the text 'Main Action Triggered!' is shown
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasExpandedInstruction("Click the Menu button", true))
            .step(2, s -> s.hasExpandedInstruction("If the Accept Cookies button is visible, then click the Accept Cookies button, else click the Main Action Button", true))
            .step(3, s -> s.hasExpandedInstruction("verify the text 'Main Action Triggered!' is shown", true));

        assertEquals("Main Action Triggered!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertEquals("Main Action Triggered!", Selenide.$("#result").text());
    }

    /**
     * Verifies the "when A and B do C otherwise D" conditional structure within a split instruction.
     */
    @NeodymiumTest
    public final void testSplittableBranchWhenOtherwise()
    {
        final String steps = """
                OPEN ${splittable.branch.url}
                Click the Menu button, and then when the Accept Cookies button is visible and contains the text "Accept Cookies", click the Accept Cookies button, otherwise click the Main Action Button, and then verify the text 'Cookies Accepted!' is shown
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasExpandedInstruction("Click the Menu button", true))
            .step(2, s -> s.hasExpandedInstruction("when the Accept Cookies button is visible and contains the text \"Accept Cookies\", click the Accept Cookies button, otherwise click the Main Action Button", true))
            .step(3, s -> s.hasExpandedInstruction("verify the text 'Cookies Accepted!' is shown", true));

        assertEquals("Cookies Accepted!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertEquals("Cookies Accepted!", Selenide.$("#result").text());
    }

    /**
     * Verifies the "when A and B do C" conditional structure (without any otherwise/else branch)
     * within a split instruction.
     */
    @NeodymiumTest
    public final void testSplittableBranchWhenNoOtherwise()
    {
        final String steps = """
                OPEN ${splittable.branch.url}
                Click the Menu button, and then when the Accept Cookies button is visible and contains the text "Accept Cookies", click the Accept Cookies button, and then verify the text 'Cookies Accepted!' is shown
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasStepsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasExpandedInstruction("Click the Menu button", true))
            .step(2, s -> s.hasExpandedInstruction("when the Accept Cookies button is visible and contains the text \"Accept Cookies\", click the Accept Cookies button", true))
            .step(3, s -> s.hasExpandedInstruction("verify the text 'Cookies Accepted!' is shown", true));

        assertEquals("Cookies Accepted!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertThat(r2)
            .hasStepsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertEquals("Cookies Accepted!", Selenide.$("#result").text());
    }
}
