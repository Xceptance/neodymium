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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI java method commands, including direct parsing,
 * indirect discovery via LLM mapping, local instance method calls, dynamic custom utility class
 * registration, and multiple default assertion types.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class JavaMethodTest extends BaseAiTest
{
    private String url;

    /**
     * Custom utility class to verify dynamic java method registration.
     */
    public static final class CustomAssertions
    {
        /**
         * Asserts that the provided argument equals "custom_value".
         *
         * @param arg the argument to validate
         * @throws AssertionError if the argument does not match expected value
         */
        public static void assertCustomValue(final String arg)
        {
            assertEquals("custom_value", arg);
        }
    }

    /**
     * Local public method to verify local test instance java method execution.
     *
     * @param arg the argument to validate
     * @throws AssertionError if the argument does not match expected value
     */
    public final void assertLocalMethod(final String arg)
    {
        assertEquals("expected_value", arg);
    }

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Neodymium.getData().put("javaMethod.test.url", this.url);
    }

    /**
     * Test invoking a default Java method directly using the 'java:' prefix.
     */
    @NeodymiumTest
    public final void testJavaMethodDirectDefault()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: assertPriceGreaterThanZero("14.96 €")
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(2);

        final StepDetails stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertFalse(stepDetails0.isReplayed());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final StepDetails stepDetails1 = r1.getSteps().get(1);
        assertTrue(stepDetails1.isDirectParse());
        assertFalse(stepDetails1.isReplayed());
        assertTrue(stepDetails1.getLlmCalls().isEmpty());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);
    }

    /**
     * Test indirect discovery of a Java method from a natural language step prompt.
     */
    @NeodymiumTest
    public final void testJavaMethodIndirectDiscovery()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                Verify using the java method assertPriceGreaterThanZero that '14.96 €' is greater than zero
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2);

        final StepDetails stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final StepDetails stepDetails1 = r1.getSteps().get(1);
        assertFalse(stepDetails1.isDirectParse());
        assertEquals(1, stepDetails1.getLlmCalls().size());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);
    }

    /**
     * Test calling a public instance method defined directly in the active test class.
     */
    @NeodymiumTest
    public final void testJavaMethodLocalInstanceMethod()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: assertLocalMethod("expected_value")
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(2);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);
    }

    /**
     * Test dynamic registration of a custom utility class and invoking its static method.
     */
    @NeodymiumTest
    public final void testJavaMethodRegisteredUtilityClass()
    {
        final String origUtilityClasses = System.getProperty("neodymium.ai.agent.javaMethod.utilityClasses");
        try
        {
            System.setProperty(
                "neodymium.ai.agent.javaMethod.utilityClasses",
                "com.xceptance.neodymium.ai.util.AiAssertions," + CustomAssertions.class.getName()
            );
            Neodymium.reloadAiConfiguration();

            final String steps = """
                    Open ${javaMethod.test.url}
                    java: assertCustomValue("custom_value")
                """;

            final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

            assertThat(r1)
                .hasLlmCalls(0)
                .hasNoEscalations()
                .hasDirectParses(2)
                .hasReplays(0)
                .hasActionsCount(2);

            this.resetBrowser();

            final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

            assertThat(r2)
                .hasLlmCalls(0)
                .hasNoEscalations()
                .hasDirectParses(0)
                .hasReplays(2)
                .hasActionsCount(2);
        }
        finally
        {
            if (origUtilityClasses != null)
            {
                System.setProperty("neodymium.ai.agent.javaMethod.utilityClasses", origUtilityClasses);
            }
            else
            {
                System.clearProperty("neodymium.ai.agent.javaMethod.utilityClasses");
            }
            Neodymium.reloadAiConfiguration();
        }
    }

    /**
     * Test other default assertion methods defined in AiAssertions.
     */
    @NeodymiumTest
    public final void testJavaMethodDefaultAssertions()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: assertNumberEqual("[\"10.00\", \"10.00\"]")
                java: assertMatchesRegex("[\"ORD-12345\", \"^ORD-[0-9]{5}$\"]")
                java: verifyCalculation("0.90 € = (14.96 € + 0.00 €) * 6.00%")
                java: verifyLessOrEqual("[\"10\", \"15\"]")
                java: assertNumberGreaterThan("[\"15.00\", \"10.00\"]")
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(6)
            .hasReplays(0)
            .hasActionsCount(6);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(6)
            .hasActionsCount(6);
    }

    /**
     * Local public static method to verify the length of the welcome message text.
     *
     * @param msg the message to validate
     * @throws AssertionError if the message length does not match expected value
     */
    public static void verifyWelcomeMessageLength(final String msg)
    {
        assertEquals(18, msg.length());
    }

    /**
     * Local public static method to verify welcome message.
     *
     * @param msg the message to validate
     * @throws AssertionError if the message does not match expected value
     */
    public static void verifyWelcomeMessage(final String msg)
    {
        assertEquals("Assert Action Test", msg);
    }

    /**
     * Local public static method to verify local test class static method execution.
     *
     * @param price the price to validate
     * @throws AssertionError if the price does not match expected value
     */
    public static void verifyStaticLocalMethod(final String price)
    {
        assertEquals("expected_static_value", price);
    }

    /**
     * Test calling a public static method defined directly in the active test class.
     */
    @NeodymiumTest
    public final void testJavaMethodLocalStaticMethodDirect()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: verifyWelcomeMessage("Assert Action Test")
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(2);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);
    }

    /**
     * Test indirect discovery and execution of a local static method passing a stored variable.
     */
    @NeodymiumTest
    public final void testJavaMethodLocalStaticMethodWithStoredVariable()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                Get the text of 'h1' and store it as 'myWelcomeMsg'
                Verify the welcome message length is 18 using the java method verifyWelcomeMessageLength with the stored 'myWelcomeMsg' as parameter
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3);
    }

    /**
     * Test indirect discovery and execution of a local static method combined with getting a value.
     */
    @NeodymiumTest
    public final void testJavaMethodLocalStaticMethodCombined()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                Get the text of 'h1' and verify the welcome message length is 18 using the java method 'verifyWelcomeMessageLength'
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3);
    }
}
