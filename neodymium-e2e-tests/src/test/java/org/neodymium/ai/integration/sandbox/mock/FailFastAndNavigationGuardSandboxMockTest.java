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
package org.neodymium.ai.integration.sandbox.mock;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Mock browser integration test for fail-fast termination ({@code fail_step}) and pre-execution
 * {@code navigate} provenance enforcement against the Aura AI Sandbox.
 *
 * @author AI-generated: Claude Opus 4.6
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
public class FailFastAndNavigationGuardSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    public void ensureProperties()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
        Neodymium.getData().put("neodymium.ai.judge.enabled", "false");
        Neodymium.getData().put("neodymium.ai.rca.enabled", "false");
        Neodymium.getData().put("neodymium.ai.navigation.guard.enabled", "true");
    }

    @AfterEach
    public void clearProperties()
    {
        Neodymium.getData().remove("neodymium.ai.global.provider");
        Neodymium.getData().remove("neodymium.ai.linter.enabled");
        Neodymium.getData().remove("neodymium.ai.judge.enabled");
        Neodymium.getData().remove("neodymium.ai.rca.enabled");
        Neodymium.getData().remove("neodymium.ai.navigation.guard.enabled");
    }

    private static MockLlmProvider bindMockProvider(final AiSession session)
    {
        final MockLlmProvider mock = new MockLlmProvider();
        session.getLlmRegistry().registerProvider(mock);
        return mock;
    }

    private static LlmResponse toolResponse(final String id, final String toolName, final ObjectNode args, final String thought)
    {
        return new LlmResponse(
                thought,
                new TokenUsage(25, 15, 40),
                "mock",
                List.of(new ToolCall(id, toolName, args)),
                thought);
    }

    /**
     * Scenario 1: Active CAPTCHA blocks a product interaction; rogue external navigation is intercepted
     * before browser execution (even with Quality Judge disabled), and {@code fail_step} escalates once
     * to RICH before failing cleanly with {@link ConclusiveFailureException}.
     */
    @Test
    public void testActiveCaptchaBlocksRogueExternalNavigationAndFailsFast()
    {
        final String captchaUrl = getSandboxUrl("/shop/sandbox/fail-fast-guard.html?mode=captcha");
        Selenide.open(captchaUrl);
        $("#challengeHeadline").shouldHave(text("Security Challenge Required"));

        final AiSession session = AiSession.selenide();
        final MockLlmProvider mock = bindMockProvider(session);

        // Turn 1: Rogue external navigate attempt -> blocked pre-execution by NavigationProvenanceInterceptor
        final ObjectNode navArgs = MAPPER.createObjectNode();
        navArgs.put("url", "https://www.ifolor.ch");
        mock.addResponse(toolResponse("call-nav-1", "navigate", navArgs, "Trying alternate country domain"));

        // Turn 2: Agent obeys provenance warning and calls fail_step below RICH -> auto-escalates once to RICH
        final ObjectNode failArgs1 = MAPPER.createObjectNode();
        failArgs1.put("reason", "Active security verification challenge is blocking the product catalog");
        mock.addResponse(toolResponse("call-fail-1", "fail_step", failArgs1, "Blocked by CAPTCHA"));

        // Turn 3: Agent confirms fail_step on escalated RICH DOM -> throws ConclusiveFailureException
        final ObjectNode failArgs2 = MAPPER.createObjectNode();
        failArgs2.put("reason", "Active security verification challenge is blocking the product catalog");
        mock.addResponse(toolResponse("call-fail-2", "fail_step", failArgs2, "Confirmed blocked in RICH DOM"));

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> session.execute("""
                    steps: |
                      Click on the first available product
                    """));

        Assertions.assertTrue(ex.getMessage().contains("Active security verification challenge"));
        Assertions.assertTrue(WebDriverRunner.url().contains("fail-fast-guard.html?mode=captcha"),
                "Browser must remain on local sandbox URL when unauthorized navigate is intercepted");
        Assertions.assertEquals(3, session.getTotalLlmCalls());
    }

    /**
     * Scenario 2: Active CAPTCHA page passes normally when the step explicitly verifies the security challenge.
     */
    @Test
    public void testActiveCaptchaWithExplicitChallengeAssertionPasses() throws Exception
    {
        final String captchaUrl = getSandboxUrl("/shop/sandbox/fail-fast-guard.html?mode=captcha");
        Selenide.open(captchaUrl);

        final AiSession session = AiSession.selenide();
        final MockLlmProvider mock = bindMockProvider(session);

        final ObjectNode assertArgs = MAPPER.createObjectNode();
        assertArgs.put("target", "#challengeHeadline");
        assertArgs.put("expectedText", "Security Challenge Required");
        mock.addResponse(toolResponse("call-assert-1", "assert_text", assertArgs, "Verifying security challenge headline"));

        final ObjectNode completeArgs = MAPPER.createObjectNode();
        completeArgs.put("summary", "Verified security challenge headline");
        mock.addResponse(toolResponse("call-complete-1", "complete_step", completeArgs, "Challenge verified"));

        session.execute("""
            steps: |
              Verify that the security challenge headline shows "Security Challenge Required"
            """);

        $("#challengeHeadline").shouldHave(text("Security Challenge Required"));
    }

    /**
     * Scenario 3: 404 error page blocking an unachievable product action at RICH context fails immediately in 1 turn.
     */
    @Test
    public void testError404PageBlockingProductActionFailsFast()
    {
        final String error404Url = getSandboxUrl("/shop/sandbox/fail-fast-guard.html?mode=404");
        Selenide.open(error404Url);
        $("#errorHeadline").shouldHave(text("404 - Page Not Found"));

        final AiSession session = AiSession.selenide();
        final MockLlmProvider mock = bindMockProvider(session);

        final ObjectNode failArgs = MAPPER.createObjectNode();
        failArgs.put("reason", "Page returned 404 - Page Not Found; Add to Cart button is unreachable");
        mock.addResponse(toolResponse("call-fail-404", "fail_step", failArgs, "404 error page blocks Add to Cart"));

        final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> session.execute("""
                    steps: |
                      (context:rich) Click the Add to Cart button
                    """));

        Assertions.assertTrue(ex.getMessage().contains("404 - Page Not Found"));
        Assertions.assertEquals(1, session.getTotalLlmCalls(), "fail_step at RICH must terminate immediately on Turn 1");
    }

    /**
     * Scenario 4: Explicit negative-testing flow on a 404 page verifies the 404 headline and clicks Back to Homepage.
     */
    @Test
    public void testError404PageWithExplicitHeadlineAndBackLinkPasses() throws Exception
    {
        final String error404Url = getSandboxUrl("/shop/sandbox/fail-fast-guard.html?mode=404");
        Selenide.open(error404Url);

        final AiSession session = AiSession.selenide();
        final MockLlmProvider mock = bindMockProvider(session);

        final ObjectNode assertArgs = MAPPER.createObjectNode();
        assertArgs.put("target", "#errorHeadline");
        assertArgs.put("expectedText", "404 - Page Not Found");
        mock.addResponse(toolResponse("call-assert-404", "assert_text", assertArgs, "Verifying 404 headline"));

        final ObjectNode clickArgs = MAPPER.createObjectNode();
        clickArgs.put("target", "#backToHomeLink");
        mock.addResponse(toolResponse("call-click-home", "click", clickArgs, "Clicking Back to Homepage"));

        final ObjectNode completeArgs = MAPPER.createObjectNode();
        completeArgs.put("summary", "Verified 404 headline and returned to storefront");
        mock.addResponse(toolResponse("call-complete-404", "complete_step", completeArgs, "Returned to catalog"));

        session.execute("""
            steps: |
              Verify the 404 headline and click Back to Homepage
            """);

        $("#catalogHeadline").shouldHave(text("Featured Collection"));
    }

    /**
     * Scenario 5: Normal product page containing a passive "Protected by reCAPTCHA" footer badge clicks product normally.
     */
    @Test
    public void testPassiveRecaptchaFooterBadgeDoesNotPreventNormalProductClick() throws Exception
    {
        final String productUrl = getSandboxUrl("/shop/sandbox/fail-fast-guard.html?mode=product");
        Selenide.open(productUrl);
        $("#recaptchaFooterBadge").shouldHave(text("Protected by reCAPTCHA"));

        final AiSession session = AiSession.selenide();
        final MockLlmProvider mock = bindMockProvider(session);

        final ObjectNode clickArgs = MAPPER.createObjectNode();
        clickArgs.put("target", "#firstProductCard");
        mock.addResponse(toolResponse("call-click-prod", "click", clickArgs, "Clicking first available product"));

        final ObjectNode completeArgs = MAPPER.createObjectNode();
        completeArgs.put("summary", "Clicked Aura Minimalist Watch");
        mock.addResponse(toolResponse("call-complete-prod", "complete_step", completeArgs, "Product selected"));

        session.execute("""
            steps: |
              Click on the first available product
            """);

        $("#productSelectedStatus").shouldHave(text("Selected: Aura Minimalist Watch"));
    }
}
