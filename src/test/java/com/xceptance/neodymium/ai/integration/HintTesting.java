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

import com.xceptance.neodymium.ai.core.AiAgentPrompts;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * A storefront integration test validating that different styles and structures
 * of inline locator hints are successfully parsed, executed, and replayed, 
 * using minimal DOM context level.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class HintTesting extends BaseAiTest
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
     * Checks that the system prompts generated for each ContextLevel do not exceed
     * the maximum length we expect. Keep the prompts lean.
     */
    @NeodymiumTest
    public final void test_SystemPromptMaxLength()
    {
        final int maxHintLength = 1800;
        final int maxAxTreeLength = 11500;
        final int maxLeanLength = 11500;
        final int maxStandardLength = 11500;
        final int maxVisualLeanLength = 13000;
        final int maxVisualLength = 13000;

        final String hintPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.HINT);
        final String axTreePrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.AXTREE);
        final String leanPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.LEAN);
        final String standardPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.STANDARD);
        final String visualLeanPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.VISUAL_LEAN);
        final String visualPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.VISUAL);

        System.out.println("HINT prompt length: " + hintPrompt.length());
        System.out.println("AXTREE prompt length: " + axTreePrompt.length());
        System.out.println("LEAN prompt length: " + leanPrompt.length());
        System.out.println("STANDARD prompt length: " + standardPrompt.length());
        System.out.println("VISUAL_LEAN prompt length: " + visualLeanPrompt.length());
        System.out.println("VISUAL prompt length: " + visualPrompt.length());

        assertTrue(hintPrompt.length() < maxHintLength, "HINT prompt exceeds limit: " + hintPrompt.length());
        assertTrue(axTreePrompt.length() < maxAxTreeLength, "AXTREE prompt exceeds limit: " + axTreePrompt.length());
        assertTrue(leanPrompt.length() < maxLeanLength, "LEAN prompt exceeds limit: " + leanPrompt.length());
        assertTrue(standardPrompt.length() < maxStandardLength, "STANDARD prompt exceeds limit: " + standardPrompt.length());
        assertTrue(visualLeanPrompt.length() < maxVisualLeanLength, "VISUAL_LEAN prompt exceeds limit: " + visualLeanPrompt.length());
        assertTrue(visualPrompt.length() < maxVisualLength, "VISUAL prompt exceeds limit: " + visualPrompt.length());
    }

    /**
     * Hint matching
     */
    @NeodymiumTest
    public final void test_MatchingHint()
    {
        final String steps = """
                Open ${posters.storefront.url}
                Click the search button (hint: #search-button)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");
        
        assertEquals("#search-button", r1.getActions().get(1).getTarget());

        // our Click step
        final var stepDetails1 = r1.getSteps().get(1);
        assertFalse(stepDetails1.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails1.getLlmCalls().get(0).getContextLevel());

        final String htmlDomContext = stepDetails1.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));

        // back to start for replay
        this.resetBrowser();

        // check LLM free replay
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0) // replays don't have parses at all
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "CLICK");
        
        assertEquals("#search-button", r2.getActions().get(1).getTarget());
    }

    /**
     * Complex Hint CSS, including selectors with 
     * [], (), '' and more
     */
    @NeodymiumTest
    public final void test_ComplexCSSHint()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Click the storefront active link (hint: ul.nav-links li:nth-child(1) a[href='index.html'])
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");

        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("index.html") || target.contains("nav-links") || target.contains("Storefront") || target.contains("brand") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final String htmlDomContext = stepDetails.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());
    }

    /**
     * Complex Hint Xpath including () in the 
     * selector
     */
    @NeodymiumTest
    public final void test_ComplexXpathHint()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Click the storefront active link (hint: xpath=(//ul[contains(@class,'nav-links')]/li/a)[1])
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");

        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("nav-links") || target.contains("brand") || target.contains("Storefront") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final String htmlDomContext = stepDetails.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());
    }

    /**
     * Complex Hint ID
     */
    @NeodymiumTest
    public final void test_IDHint()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Click the featured bear image (hint: #featured-bear-img)
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");
        
        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("featured-bear-img"));

        final var stepDetails = r1.getSteps().get(1);
        final String htmlDomContext = stepDetails.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());
    }

    /**
     * Missing Hint value
     */
    @NeodymiumTest
    public final void test_MissingHintValue()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Click the search button (hint: )
                """, VerificationMode.LIVE_LLM);

        // Starts at HINT context level, fails due to empty context and missing hint selector,
        // escalates to AXTREE level, makes 2nd LLM call and succeeds.
        assertThat(r1)
            .hasLlmCalls(2)
            .hasEscalations(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");
        
        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("search-button") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final var firstCall = stepDetails.getLlmCalls().get(0);
        final var secondCall = stepDetails.getLlmCalls().get(1);

        assertEquals(ContextLevel.HINT, firstCall.getContextLevel());
        assertTrue(firstCall.getHtmlDomContext().contains("Page URL:"));
        assertTrue(firstCall.getHtmlDomContext().contains("Page Title:"));
        assertFalse(firstCall.getHtmlDomContext().contains("=== Interactive Elements ==="));

        assertEquals(ContextLevel.AXTREE, secondCall.getContextLevel());
    }

    /**
     * Hint in different legal ways to write it including
     * Such as ( hint: css) or ( hint : css ) and so on
     */
    @NeodymiumTest
    public final void test_ComplexIDHint()
    {
        // 1. Whitespace variations:
        // Space before colon / hint: ( hint: #search-box ) and ( hint : #search-button )
        // Note: These will not trigger HINT context level initially but LLM still extracts them from the prompt.
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Click the search box ( hint: #search-box )
                Click the search button ( hint : #search-button )
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoEscalations()
            .hasActionsCount(3);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");
        assertThat(r1).hasAction(2, "CLICK");
        
        final String targetBox1 = r1.getActions().get(1).getTarget();
        final String targetBtn1 = r1.getActions().get(2).getTarget();
        assertTrue(targetBox1.contains("search-box") || targetBox1.contains("xc_"));
        assertTrue(targetBtn1.contains("search-button") || targetBtn1.contains("xc_"));

        // 2. Case variations: (Hint: #search-box) and (HINT: #search-button)
        // These will successfully trigger ContextLevel.HINT context level.
        this.resetBrowser();

        final AiExecutionResult r2 = runAi("""
                Open ${posters.storefront.url}
                Click the search box (Hint: #search-box)
                Click the search button (HINT: #search-button)
                """, VerificationMode.LIVE_LLM);

        assertThat(r2)
            .hasLlmCalls(2)
            .hasNoEscalations()
            .hasActionsCount(3);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "CLICK");
        assertThat(r2).hasAction(2, "CLICK");

        final String targetBox2 = r2.getActions().get(1).getTarget();
        final String targetBtn2 = r2.getActions().get(2).getTarget();
        assertTrue(targetBox2.contains("search-box"));
        assertTrue(targetBtn2.contains("search-button"));

        final var step1 = r2.getSteps().get(1);
        final var step2 = r2.getSteps().get(2);

        assertEquals(ContextLevel.HINT, step1.getLlmCalls().get(0).getContextLevel());
        assertEquals(ContextLevel.HINT, step2.getLlmCalls().get(0).getContextLevel());

        final String htmlDomContext1 = step1.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext1.contains("Page URL:"));
        assertTrue(htmlDomContext1.contains("Page Title:"));
        assertFalse(htmlDomContext1.contains("=== Interactive Elements ==="));
    }

    /**
     * Invalid Hint selector causing self-healing escalation
     */
    @NeodymiumTest
    public final void test_InvalidHintEscalation()
    {
        final AiExecutionResult r1 = runAi("""
                Open ${posters.storefront.url}
                Click the search button (hint: #non-existent-button)
                """, VerificationMode.LIVE_LLM);

        // Starts at HINT context level, LLM generates action using the hint (#non-existent-button),
        // ActionExecutor execution fails because element is not found,
        // context level escalates to AXTREE (which has DOM),
        // LLM generates correct action (#search-button), and execution succeeds.
        assertThat(r1)
            .hasLlmCalls(2)
            .hasEscalations(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CLICK");
        
        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("search-button") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final var firstCall = stepDetails.getLlmCalls().get(0);
        final var secondCall = stepDetails.getLlmCalls().get(1);

        assertEquals(ContextLevel.HINT, firstCall.getContextLevel());
        assertTrue(firstCall.getHtmlDomContext().contains("Page URL:"));
        assertTrue(firstCall.getHtmlDomContext().contains("Page Title:"));
        assertFalse(firstCall.getHtmlDomContext().contains("=== Interactive Elements ==="));

        assertEquals(ContextLevel.AXTREE, secondCall.getContextLevel());
        assertTrue(secondCall.getHtmlDomContext().contains("=== Interactive Elements ===") || secondCall.getHtmlDomContext().contains("Store Catalog"));
    }

    /**
     * Type action hint.
     */
    @NeodymiumTest
    public final void test_TypeHint()
    {
        final String steps = """
                Open ${posters.storefront.url}
                Type "bear" into search input (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "TYPE", "#search-box", "bear");

        final var stepDetails = r1.getSteps().get(1);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "TYPE", "#search-box", "bear");
    }

    /**
     * Clear action hint.
     */
    @NeodymiumTest
    public final void test_ClearHint()
    {
        final String steps = """
                Open ${posters.storefront.url}
                Type "bear" into search input (hint: #search-box)
                Clear the search input (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "TYPE", "#search-box", "bear");
        assertThat(r1).hasAction(2, "CLEAR", "#search-box");

        final var stepDetails = r1.getSteps().get(2);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "TYPE", "#search-box", "bear");
        assertThat(r2).hasAction(2, "CLEAR", "#search-box");
    }

    /**
     * Select action hint.
     */
    @NeodymiumTest
    public final void test_SelectHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                Open %s
                Select "Price: Low to High" from sorting dropdown (hint: #sort-select-input)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "SELECT", "#sort-select-input", "Price: Low to High");

        final var stepDetails = r1.getSteps().get(1);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "SELECT", "#sort-select-input", "Price: Low to High");
    }

    /**
     * Check action hint.
     */
    @NeodymiumTest
    public final void test_CheckHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                Open %s
                Check the tops category checkbox (hint: #filter-cat-tops)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "CHECK", "#filter-cat-tops");

        final var stepDetails = r1.getSteps().get(1);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "CHECK", "#filter-cat-tops");
    }

    /**
     * Hover action hint.
     */
    @NeodymiumTest
    public final void test_HoverHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                Open %s
                Hover over the Tops navigation link (hint: #nav-link-tops)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "HOVER", "#nav-link-tops");

        final var stepDetails = r1.getSteps().get(1);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "HOVER", "#nav-link-tops");
    }

    /**
     * Assert action hint.
     */
    @NeodymiumTest
    public final void test_AssertHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                Open %s
                Assert that the page title is visible (hint: #plp-page-title)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "ASSERT", "#plp-page-title");

        final var stepDetails = r1.getSteps().get(1);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "ASSERT", "#plp-page-title");
    }

    /**
     * Scroll action hint.
     */
    @NeodymiumTest
    public final void test_ScrollHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                Open %s
                Scroll to the email input field in newsletter section (hint: #newsletter-email-input)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "SCROLL", "#newsletter-email-input");

        final var stepDetails = r1.getSteps().get(1);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "SCROLL", "#newsletter-email-input");
    }

    /**
     * Key press action hint.
     */
    @NeodymiumTest
    public final void test_KeyPressHint()
    {
        final String steps = """
                Open ${posters.storefront.url}
                Type "bear" into the search box (hint: #search-box)
                Press ENTER on the search box (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3);

        assertThat(r1).hasAction(0, "NAVIGATE");
        assertThat(r1).hasAction(1, "TYPE", "#search-box", "bear");
        assertThat(r1).hasAction(2, "KEY_PRESS", "#search-box", "Enter");

        final var stepDetails = r1.getSteps().get(2);
        assertFalse(stepDetails.isDirectParse());
        assertEquals(ContextLevel.HINT, stepDetails.getLlmCalls().get(0).getContextLevel());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3);

        assertThat(r2).hasAction(0, "NAVIGATE");
        assertThat(r2).hasAction(1, "TYPE", "#search-box", "bear");
        assertThat(r2).hasAction(2, "KEY_PRESS", "#search-box", "Enter");
    }
}
