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

import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * A storefront integration test validating that different styles and structures
 * of inline locator hints are successfully parsed, executed, and replayed, 
 * using minimal DOM context level.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
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
        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html", server.getPort());
        Neodymium.getData().put("posters.storefront.url", this.url);
    }

    /**
     * Hint matching
     */
    @NeodymiumTest
    public final void test_MatchingHint()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Click the search button (hint: #search-button)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));
        
        assertEquals("#search-button", r1.getActions().get(1).getTarget());

        // our Click step
        final var stepDetails1 = r1.getSteps().get(1);
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
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0) // replays don't have parses at all
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK");
        
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
                OPEN ${posters.storefront.url}
                Click the storefront active link (hint: ul.nav-links li:nth-child(1) a[href='index.html'])
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("index.html") || target.contains("nav-links") || target.contains("Storefront") || target.contains("brand") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final String htmlDomContext = stepDetails.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));
    }

    /**
     * Complex Hint Xpath including () in the 
     * selector
     */
    @NeodymiumTest
    public final void test_ComplexXpathHint()
    {
        final AiExecutionResult r1 = runAi("""
                OPEN ${posters.storefront.url}
                Click the storefront active link (hint: xpath=(//ul[contains(@class,'nav-links')]/li/a)[1])
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("nav-links") || target.contains("brand") || target.contains("Storefront") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final String htmlDomContext = stepDetails.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));
    }

    /**
     * Complex Hint ID
     */
    @NeodymiumTest
    public final void test_IDHint()
    {
        final AiExecutionResult r1 = runAi("""
                OPEN ${posters.storefront.url}
                Click the featured bear image (hint: #featured-bear-img)
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));
        
        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("featured-bear-img"));

        final var stepDetails = r1.getSteps().get(1);
        final String htmlDomContext = stepDetails.getLlmCalls().get(0).getHtmlDomContext();
        assertTrue(htmlDomContext.contains("Page URL:"));
        assertTrue(htmlDomContext.contains("Page Title:"));
        assertFalse(htmlDomContext.contains("=== Interactive Elements ==="));
    }

    /**
     * Missing Hint value
     */
    @NeodymiumTest
    public final void test_MissingHintValue()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Click the search button (hint: )
                """;
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        // Starts at HINT context level, fails due to empty context and missing hint selector,
        // escalates to AXTREE level, makes 2nd LLM call and succeeds.
        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasEscalations(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .hasContextLevel(1, ContextLevel.AXTREE)
            .step(1, s -> s.isLlm(2));

        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("search-button") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final var firstCall = stepDetails.getLlmCalls().get(0);

        assertTrue(firstCall.getHtmlDomContext().contains("Page URL:"));
        assertTrue(firstCall.getHtmlDomContext().contains("Page Title:"));
        assertFalse(firstCall.getHtmlDomContext().contains("=== Interactive Elements ==="));

        this.resetBrowser();

        // let's replay
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasEscalations(0)
            .hasActionsCount(2);
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
                OPEN ${posters.storefront.url}
                Click the search box ( hint: #search-box )
                Click the search button ( hint : #search-button )
                """, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "CLICK")
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1));
        
        final String targetBox1 = r1.getActions().get(1).getTarget();
        final String targetBtn1 = r1.getActions().get(2).getTarget();
        assertTrue(targetBox1.contains("search-box") || targetBox1.contains("xc_"));
        assertTrue(targetBtn1.contains("search-button") || targetBtn1.contains("xc_"));

        // 2. Case variations: (Hint: #search-box) and (HINT: #search-button)
        // These will successfully trigger ContextLevel.HINT context level.
        this.resetBrowser();

        final AiExecutionResult r2 = runAi("""
                OPEN ${posters.storefront.url}
                Click the search box (Hint: #search-box)
                Click the search button (HINT: #search-button)
                """, VerificationMode.LIVE_LLM);

        assertThat(r2)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .hasContextLevel(1, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1));

        final String targetBox2 = r2.getActions().get(1).getTarget();
        final String targetBtn2 = r2.getActions().get(2).getTarget();
        assertTrue(targetBox2.contains("search-box"));
        assertTrue(targetBtn2.contains("search-button"));

        final var step1 = r2.getSteps().get(1);

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
                OPEN ${posters.storefront.url}
                Click the search button (hint: #non-existent-button)
                """, VerificationMode.LIVE_LLM);

        // Starts at HINT context level, LLM generates action using the hint (#non-existent-button),
        // ActionExecutor execution fails because element is not found,
        // context level escalates to AXTREE (which has DOM),
        // LLM generates correct action (#search-button), and execution succeeds.
        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasEscalations(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasContextLevel(0, ContextLevel.HINT)
            .hasContextLevel(1, ContextLevel.AXTREE)
            .step(1, s -> s.isLlm(2));
        
        final String target = r1.getActions().get(1).getTarget();
        assertTrue(target.contains("search-button") || target.contains("xc_"));

        final var stepDetails = r1.getSteps().get(1);
        final var firstCall = stepDetails.getLlmCalls().get(0);
        final var secondCall = stepDetails.getLlmCalls().get(1);

        assertTrue(firstCall.getHtmlDomContext().contains("Page URL:"));
        assertTrue(firstCall.getHtmlDomContext().contains("Page Title:"));
        assertFalse(firstCall.getHtmlDomContext().contains("=== Interactive Elements ==="));

        assertTrue(secondCall.getHtmlDomContext().contains("=== Interactive Elements ===") || secondCall.getHtmlDomContext().contains("Store Catalog"));
    }

    /**
     * Type action hint.
     */
    @NeodymiumTest
    public final void test_TypeHint()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Type "bear" into search input (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear");
    }

    /**
     * Clear action hint.
     */
    @NeodymiumTest
    public final void test_ClearHint()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Type "bear" into search input (hint: #search-box)
                Clear the search input (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "CLEAR", "#search-box")
            .hasContextLevel(1, ContextLevel.HINT)
            .step(2, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "CLEAR", "#search-box");
    }

    /**
     * Select action hint.
     */
    @NeodymiumTest
    public final void test_SelectHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                OPEN %s
                Select "Price: Low to High" from sorting dropdown (hint: #sort-select-input)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "SELECT", "#sort-select-input", "Price: Low to High")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "SELECT", "#sort-select-input", "Price: Low to High");
    }

    /**
     * Check action hint.
     */
    @NeodymiumTest
    public final void test_CheckHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                OPEN %s
                Check the tops category checkbox (hint: #filter-cat-tops)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CHECK", "#filter-cat-tops")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CHECK", "#filter-cat-tops");
    }

    /**
     * Hover action hint.
     */
    @NeodymiumTest
    public final void test_HoverHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                OPEN %s
                Hover over the Tops navigation link (hint: #nav-link-tops)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "HOVER", "#nav-link-tops")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "HOVER", "#nav-link-tops");
    }

    /**
     * Assert action hint.
     */
    @NeodymiumTest
    public final void test_AssertHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                OPEN %s
                Assert that the page title is visible (hint: #plp-page-title)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "ASSERT", "#plp-page-title")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "ASSERT", "#plp-page-title");
    }

    /**
     * Scroll action hint.
     */
    @NeodymiumTest
    public final void test_ScrollHint()
    {
        final String targetUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/plp-perfect.html", server.getPort());
        final String steps = String.format("""
                OPEN %s
                Scroll to the email input field in newsletter section (hint: #newsletter-email-input)
                """, targetUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "SCROLL", "#newsletter-email-input")
            .hasContextLevel(0, ContextLevel.HINT)
            .step(1, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "SCROLL", "#newsletter-email-input");
    }

    /**
     * Key press action hint.
     */
    @NeodymiumTest
    public final void test_KeyPressHint()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Type "bear" into the search box (hint: #search-box)
                Press ENTER on the search box (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "KEY_PRESS", "#search-box", "Enter")
            .hasContextLevel(1, ContextLevel.HINT)
            .step(2, s -> s.isLlm(1));

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "KEY_PRESS", "#search-box", "Enter");
    }

    /**
     * Key press action hint with no target in instruction text.
     */
    @NeodymiumTest
    public final void test_KeyPressHintNoTargetInText()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Type "bear" into the search box (hint: #search-box)
                Press the ENTER key (hint: #search-box)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "KEY_PRESS", "#search-box", "Enter")
            .hasContextLevel(1, ContextLevel.HINT)
            .step(2, s -> s.isLlm(1));

        // back to start for replay
        this.resetBrowser();

        // check LLM free replay
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "KEY_PRESS", "#search-box", "Enter");
    }

    /**
     * Test compound step with hints to verify they stay intact during split.
     */
    @NeodymiumTest
    public final void test_HintSplit()
    {
        final String steps = """
                OPEN ${posters.storefront.url}
                Type "bear" into search input (hint: #search-box) and click search button (hint: #search-button)
                """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "CLICK", "#search-button");

        assertEquals("#search-box", r1.getActions().get(1).getTarget());
        assertEquals("#search-button", r1.getActions().get(2).getTarget());

        // Reset and check Replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE", "#search-box", "bear")
            .hasAction(2, "CLICK", "#search-button");
    }
}
