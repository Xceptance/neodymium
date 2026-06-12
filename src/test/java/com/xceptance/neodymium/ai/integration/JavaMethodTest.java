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
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
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

    private String storeUrl;

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

        /**
         * Asserts shadowed method.
         *
         * @param val the value
         */
        public static void assertShadowedMethod(final String val)
        {
            throw new AssertionError("Utility class method should not be called: " + val);
        }
    }

    private static boolean overloadedNoArgsCalled = false;
    private static boolean overloadedWithArgCalled = false;

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
     * Local method to verify shadowed static method execution.
     *
     * @param val the value to validate
     * @throws AssertionError if the value does not match expected value
     */
    public static void assertShadowedMethod(final String val)
    {
        assertEquals("shadow_value", val);
    }

    /**
     * Private helper method. Should not be visible or callable by JAVA_METHOD.
     *
     * @param arg the argument
     */
    private static void assertPrivateMethod(final String arg)
    {
        // Not reachable
    }

    /**
     * Static helper method with Integer parameter. Should be rejected by JAVA_METHOD.
     *
     * @param value the integer value
     */
    public static void assertWrapperParam(final Integer value)
    {
        // Not reachable
    }

    /**
     * Public static method that always fails.
     *
     * @param arg the argument
     * @throws AssertionError always
     */
    public static void assertFailingMethod(final String arg)
    {
        throw new AssertionError("Forced assertion failure: " + arg);
    }

    /**
     * Public static method without parameters for testing overloading.
     */
    public static void assertOverloaded()
    {
        overloadedNoArgsCalled = true;
    }

    /**
     * Public static method with a String parameter for testing overloading.
     *
     * @param arg the string argument
     */
    public static void assertOverloaded(final String arg)
    {
        overloadedWithArgCalled = true;
        assertEquals("test_val", arg);
    }

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        this.storeUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/homepage-normal.html", server.getPort());
        Neodymium.getData().put("javaMethod.test.url", this.url);
        Neodymium.getData().put("store.url", this.url);
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
            .hasNoPesapCalls()
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
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);
    }

    /**
     * Test invoking a default Java method directly using the 'java:' prefix.
     * Using the paranthesis syntax.
     */
    @NeodymiumTest
    public final void testJavaMethodDirectParanthesis()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                (java: assertPriceGreaterThanZero("14.96 €"))
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
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
            .hasNoPesapCalls()
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
            .hasPesapCalls(1)
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
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);
    }

    /**
     * Compares Java method indirect discovery execution with and without PESAP enabled, asserting equivalent results.
     */
    @NeodymiumTest
    public final void testJavaMethodIndirectDiscovery_PesapComparison()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                Verify using the java method assertPriceGreaterThanZero that '14.96 €' is greater than zero
            """;

        final AiExecutionResult rWithPesap = runAi(steps, VerificationMode.LIVE_LLM, true);
        final StepDetails stepWithPesap = rWithPesap.getSteps().get(1);
        assertFalse(stepWithPesap.isDirectParse());

        this.resetBrowser();

        final AiExecutionResult rWithoutPesap = runAi(steps, VerificationMode.LIVE_LLM, false);
        final StepDetails stepWithoutPesap = rWithoutPesap.getSteps().get(1);
        assertFalse(stepWithoutPesap.isDirectParse());

        assertEquals(rWithPesap.getActions().size(), rWithoutPesap.getActions().size());
        assertEquals(rWithPesap.getActions().get(0).getType(), rWithoutPesap.getActions().get(0).getType());
        assertEquals(rWithPesap.getActions().get(1).getType(), rWithoutPesap.getActions().get(1).getType());
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
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(2);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
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
                .hasNoPesapCalls()
                .hasNoEscalations()
                .hasDirectParses(2)
                .hasReplays(0)
                .hasActionsCount(2);

            this.resetBrowser();

            final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

            assertThat(r2)
                .hasLlmCalls(0)
                .hasNoPesapCalls()
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
                java: assertNumbersEqual("[\"10.00\", \"10.00\"]")
                java: assertMatchesRegex("[\"ORD-12345\", \"^ORD-[0-9]{5}$\"]")
                java: assertCalculation("0.90 € = (14.96 € + 0.00 €) * 6.00%")
                java: verifyLessOrEqual("[\"10\", \"15\"]")
                java: assertNumberGreaterThan("[\"15.00\", \"10.00\"]")
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(6)
            .hasReplays(0)
            .hasActionsCount(6);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
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
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(2);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
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
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
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
                Get the text of 'h1' and verify its length is 18 using the java method 'verifyWelcomeMessageLength'
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3);
    }

    /**
     * Local parameterless method for testing inline direct invocation.
     */
    public final void assertParameterless()
    {
        assertTrue(true);
    }

    /**
     * Test direct resolution of inline java: method calls using different conventions.
     */
    @NeodymiumTest
    public final void testJavaMethodInlineConvention()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                Verify the price is correct (java: assertPriceGreaterThanZero("14.96 €"))
                Run a standalone parenthesized command (java: assertLocalMethod("expected_value"))
                Call a parameterless method (java: assertParameterless())
                Call a parameterless method without parens (java: assertParameterless)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(5)
            .hasReplays(0)
            .hasActionsCount(5);

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(5)
            .hasActionsCount(5);
    }

    /**
     * Test variable interpolation inside inline direct calls.
     */
    @NeodymiumTest
    public final void testJavaMethodVariableInterpolation()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                Get the text of 'h1' and store it as 'headerText'
                java: verifyWelcomeMessageLength("${headerText}")
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasActionsCount(3);
    }

    /**
     * Test that test class static/instance methods shadow registered utility methods.
     */
    @NeodymiumTest
    public final void testJavaMethodShadowingPrecedence()
    {
        final String origUtilityClasses = System.getProperty("neodymium.ai.agent.javaMethod.utilityClasses");
        try
        {
            System.setProperty(
                "neodymium.ai.agent.javaMethod.utilityClasses",
                CustomAssertions.class.getName()
            );
            Neodymium.reloadAiConfiguration();

            final String steps = """
                    Open ${javaMethod.test.url}
                    java: assertShadowedMethod("shadow_value")
                """;

            final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

            assertThat(r1)
                .hasDirectParses(2)
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

    private void assertExecutionThrows(final String steps, final Class<? extends Throwable> expectedCause)
    {
        final Throwable thrown = org.junit.jupiter.api.Assertions.assertThrows(
            Throwable.class,
            () -> runAi(steps, VerificationMode.LIVE_LLM)
        );

        Throwable cause = thrown;
        boolean found = false;
        while (cause != null)
        {
            if (expectedCause.isInstance(cause))
            {
                found = true;
                break;
            }
            cause = cause.getCause();
        }
        assertTrue(found, "Expected " + expectedCause.getSimpleName() + " in cause chain of: " + thrown);
    }

    /**
     * Test that invoking a private method throws ActionExecutionException.
     */
    @NeodymiumTest
    public final void testJavaMethodVisibilityError()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: assertPrivateMethod("test")
            """;

        assertExecutionThrows(steps, ActionExecutionException.class);
    }

    /**
     * Test that methods with non-String parameter types are rejected.
     */
    @NeodymiumTest
    public final void testJavaMethodWrapperParamError()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: assertWrapperParam("123")
            """;

        assertExecutionThrows(steps, ActionExecutionException.class);
    }

    /**
     * Test that AssertionError thrown inside custom methods is propagated correctly.
     */
    @NeodymiumTest
    public final void testJavaMethodAssertionErrorPropagation()
    {
        final String steps = """
                Open ${javaMethod.test.url}
                java: assertFailingMethod("fail")
            """;

        assertExecutionThrows(steps, AssertionError.class);
    }

    /**
     * Test preference of String overload over parameterless overload when argument is passed.
     */
    @NeodymiumTest
    public final void testJavaMethodOverloadingResolution()
    {
        overloadedNoArgsCalled = false;
        overloadedWithArgCalled = false;

        final String stepsArg = """
                Open ${javaMethod.test.url}
                java: assertOverloaded("test_val")
            """;

        runAi(stepsArg, VerificationMode.LIVE_LLM);
        assertTrue(overloadedWithArgCalled);
        assertFalse(overloadedNoArgsCalled);

        overloadedNoArgsCalled = false;
        overloadedWithArgCalled = false;

        final String stepsNoArg = """
                Open ${javaMethod.test.url}
                java: assertOverloaded
            """;

        runAi(stepsNoArg, VerificationMode.LIVE_LLM);
        assertTrue(overloadedNoArgsCalled);
        assertFalse(overloadedWithArgCalled);
    }
}
