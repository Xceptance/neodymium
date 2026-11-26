/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiOutcomeVerification;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Real-browser headless Chrome mock-LLM integration test reproducing GitLab #172 on the
 * VÉRLA storefront. Verifies that pure {@code (visual)} and {@code (visual: full)} steps
 * record visual SSIM baselines matching their declared capture scope and replay cleanly under
 * {@link ExecutionMode#REPLAY_STRICT}.
 *
 * @author AI-generated: Gemini
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiJudge(false)
@AiOutcomeVerification(value = false, failOnError = false)
@AiPlaybook(value = "programmatic", recordingDirectory = "target/playbooks/integration/sandbox", recordingFileName = "visual_baseline_fullpage_scope_mock_playbook")
public class VisualBaselineFullPageScopeSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String pageUrl;

    private MockLlmProvider mockProvider;

    /**
     * Constructs a default VisualBaselineFullPageScopeSandboxMockTest instance.
     */
    public VisualBaselineFullPageScopeSandboxMockTest()
    {
    }

    /**
     * Configures the global mock LLM provider and disables pre-flight linting before tests run.
     */
    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    /**
     * Resets storefront inventory, configures dynamic VÉRLA URL, and clears queued mock responses.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupStorefront(final AiSession session)
    {
        if (server != null)
        {
            server.resetInventory();
        }
        this.pageUrl = String.format("https://localhost:%d/verla-perfect/index.html", server.getHttpsPort());
        session.data().putDynamic("verla.page.url", this.pageUrl, false);

        LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (!(provider instanceof MockLlmProvider))
        {
            provider = new MockLlmProvider();
        }
        this.mockProvider = (MockLlmProvider) provider;
        session.getLlmRegistry().registerProvider(this.mockProvider);
        session.getLlmRegistry().setDefaultProvider(this.mockProvider);
        this.mockProvider.clearResponses();
    }

    /**
     * Verifies that a pure viewport {@code (visual)} step whose live multi-turn loop escalates
     * to {@code VISUAL_LEAN} and triggers a full-page {@code KEY_LAST_STATE} capture via
     * {@code query_dom} still hashes the viewport {@code KEY_POST_ACTION_STATE} with
     * {@code fullPage=false} and passes {@link ExecutionMode#REPLAY_STRICT}.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testPureViewportVisualStepWithEscalatedQueryDomReplaysStrictly(final AiSession session) throws Exception
    {
        queueOpenStorefrontResponse();

        // Turn 1 of pure (visual) step: screenshot tool escalates activeContextLevel to VISUAL_LEAN
        final ObjectNode screenshotArgs = MAPPER.createObjectNode();
        screenshotArgs.put("fullPage", true);
        final ToolCall screenshotCall = new ToolCall("call_screenshot", "screenshot", screenshotArgs);
        this.mockProvider.addResponse(new LlmResponse("", null, "mock", List.of(screenshotCall),
            "Escalating visual context via screenshot tool"));

        // Turn 2: read-only query_dom discovery tool leaves step.actions empty while triggering full-page KEY_LAST_STATE capture
        final ObjectNode queryArgs = MAPPER.createObjectNode();
        queryArgs.put("selector", ".hero-section");
        final ToolCall queryDomCall = new ToolCall("call_query_dom", "query_dom", queryArgs);
        this.mockProvider.addResponse(new LlmResponse("", null, "mock", List.of(queryDomCall),
            "Inspecting DOM around hero section during visual check"));

        // Turn 3: complete_step finishes the pure visual step
        final ObjectNode completeVisualArgs = MAPPER.createObjectNode();
        completeVisualArgs.put("summary", "Country flag verified visually");
        final ToolCall completeVisualCall = new ToolCall("call_complete_visual", "complete_step", completeVisualArgs);
        this.mockProvider.addResponse(new LlmResponse("", null, "mock", List.of(completeVisualCall),
            "Visual check satisfied"));

        session.execute("""
            steps: |
              Öffne ${verla.page.url}
              Die gezeigte Landesflagge ist die Flagge von "United States" (visual).
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }

    /**
     * Verifies that a pure full-page {@code (visual: full)} step records a full-page SSIM
     * baseline and passes {@link ExecutionMode#REPLAY_STRICT}.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook(value = "programmatic", recordingDirectory = "target/playbooks/integration/sandbox", recordingFileName = "visual_baseline_fullpage_true_mock_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testPureFullPageVisualStepReplaysStrictly(final AiSession session) throws Exception
    {
        queueOpenStorefrontResponse();

        final ObjectNode completeFullArgs = MAPPER.createObjectNode();
        completeFullArgs.put("summary", "Full storefront verified visually");
        final ToolCall completeFullCall = new ToolCall("call_complete_full", "complete_step", completeFullArgs);
        this.mockProvider.addResponse(new LlmResponse("", null, "mock", List.of(completeFullCall),
            "Full-page visual verification satisfied"));

        session.execute("""
            steps: |
              Öffne ${verla.page.url}
              Verify full storefront layout (visual: full)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }

    private void queueOpenStorefrontResponse()
    {
        final ObjectNode navArgs = MAPPER.createObjectNode();
        navArgs.put("url", this.pageUrl);
        final ToolCall navCall = new ToolCall("call_nav", "navigate", navArgs);

        final ObjectNode completeNavArgs = MAPPER.createObjectNode();
        completeNavArgs.put("summary", "Opened VÉRLA storefront");
        final ToolCall completeNavCall = new ToolCall("call_complete_nav", "complete_step", completeNavArgs);

        this.mockProvider.addResponse(new LlmResponse("", null, "mock", List.of(navCall, completeNavCall),
            "Navigate to VÉRLA storefront"));
    }
}
