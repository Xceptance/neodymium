/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI include commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("llm")
public class IncludeTest extends BaseAiTest
{
    /**
     * Set up storefront url parameter and resource path before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        final String url = String.format("http://localhost:%d/IncludeTest/testInclude.html", server.getPort());
        Neodymium.getData().put("include.test.url", url);
        Neodymium.getData().put("neodymium.classpathResourcePath", "com/xceptance/neodymium/ai/integration/IncludeTest.yaml");
    }

    @NeodymiumTest
    public final void testConditionalIncludeThenBranch() throws Exception
    {
        testWith("""
            OPEN ${include.test.url}
            If the element (hint: #element) is visible, then _include: fragments/testConditionalIncludeThenBranch_sub.steps else _include: fragments/testConditionalIncludeThenBranch_else.steps""",
                4, "B Clicked");
    }

    @NeodymiumTest
    public final void testConditionalIncludeElseBranch() throws Exception
    {
        testWith("""
            OPEN ${include.test.url}?hideElement=true
            If the element (hint: #element) is visible, then _include: fragments/testConditionalIncludeElseBranch_sub.steps else _include: fragments/testConditionalIncludeElseBranch_else.steps""",
                4, "D Clicked");
    }

    @NeodymiumTest
    public final void testMixedStaticAndDynamicIncludes() throws Exception
    {
        Neodymium.getData().put("username", "dynamic_nested_user");
        testWith("""
            OPEN ${include.test.url}
            If the element (hint: #element) is visible, then _include: fragments/testMixedStaticAndDynamicIncludes_parent.steps""",
                4, "Child Clicked");
    }

    @NeodymiumTest
    public final void testStoredVariableResolutionInIncludes() throws Exception
    {
        testWith("""
            OPEN ${include.test.url}
            Store the order ID value as variable 'myOrder' (hint: #order-id)
            If the element (hint: #element) is visible, then _include: fragments/testStoredVariableResolutionInIncludes_useVar.steps""",
                4, null);
    }

    @NeodymiumTest
    public final void testDynamicCircularInclusionGuard() throws Exception
    {
        final String steps = """
            OPEN ${include.test.url}
            If the element (hint: #element) is visible, then _include: fragments/testDynamicCircularInclusionGuard_loopA.steps""";
        final Throwable exception = assertThrows(Throwable.class, () ->
        {
            runAi(steps, VerificationMode.LIVE_LLM);
        });

        final boolean hasCircularMsg = exception.getMessage().contains("Circular dynamic inclusion detected")
                || (exception.getCause() != null && exception.getCause().getMessage().contains("Circular dynamic inclusion detected"));
        assertTrue(hasCircularMsg);
    }

    private void testWith(final String steps, final int expectedStepCount, final String expectedVerifiedText) throws Exception
    {
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertEquals(expectedStepCount, r1.getSteps().size());
        if (expectedVerifiedText != null)
        {
            assertEquals(expectedVerifiedText, Selenide.$("#verified").text());
        }

        // reset browser and replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertEquals(expectedStepCount, r2.getSteps().size());
        if (expectedVerifiedText != null)
        {
            assertEquals(expectedVerifiedText, Selenide.$("#verified").text());
        }
    }
}

