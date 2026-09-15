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
package org.neodymium.ai.playbook.linter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.session.AiSession;
import org.neodymium.util.Neodymium;

/**
 * Unit tests for {@link PostFlightPlaybookLinter}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PostFlightPlaybookLinterTest
{
    private String originalPostFlightEnabled;

    @BeforeEach
    public void setUp()
    {
        Neodymium.clearThreadContext();
        this.originalPostFlightEnabled = System.getProperty("neodymium.ai.linter.postFlight.enabled");
    }

    @AfterEach
    public void tearDown()
    {
        Neodymium.clearThreadContext();
        if (this.originalPostFlightEnabled != null)
        {
            System.setProperty("neodymium.ai.linter.postFlight.enabled", this.originalPostFlightEnabled);
        }
        else
        {
            System.clearProperty("neodymium.ai.linter.postFlight.enabled");
        }
        System.clearProperty("neodymium.ai.postflight.linter.enabled");
        System.clearProperty("neodymium.ai.linter.enabled");
        AiConfiguration.resetInstance();
        ExecutionContext.setActiveContext(null);
    }

    @Test
    @DisplayName("Verify post-flight linter is disabled by default")
    public void testDisabledByDefault()
    {
        Neodymium.clearThreadContext();
        System.clearProperty("neodymium.ai.linter.postFlight.enabled");
        System.clearProperty("neodymium.ai.postflight.linter.enabled");
        AiConfiguration.resetInstance();

        assertFalse(AiConfiguration.getInstance().isPostFlightLinterEnabled());

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("Hover over cart and click checkout");
        s.setActions(List.of(new Action("CLICK", "#cart", List.of(), "Click cart", "", true),
                             new Action("CLICK", "#checkout", List.of(), "Click checkout", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertTrue(findings.isEmpty());
    }

    @Test
    @DisplayName("Verify post-flight linter is bypassed in replay mode")
    public void testBypassedInReplayMode()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("Click login");
        s.setActions(List.of(new Action("CLICK", "#a", List.of(), "", "", true),
                             new Action("CLICK", "#b", List.of(), "", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertTrue(findings.isEmpty());
    }

    @Test
    @DisplayName("Verify EMPIRICAL_MULTI_ACTION triggers when a flat step executes multiple mutating actions")
    public void testEmpiricalMultiActionDetection()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);

        final PlaybookStep s = new PlaybookStep("Open dropdown and pick Germany");
        s.setLineNumber(12);
        s.setSourceFile("playbooks/country.yaml");
        s.setActions(List.of(
            new Action("CLICK", "#country-dropdown", List.of(), "Open dropdown", "", true),
            new Action("CLICK", "#item-de", List.of(), "Pick Germany", "", true)
        ));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(1, f.stepIndex());
        assertEquals(12, f.lineNumber());
        assertEquals("playbooks/country.yaml", f.sourceFile());
        assertEquals(LinterCategory.EMPIRICAL_MULTI_ACTION, f.category());
        assertEquals(LinterSeverity.WARNING, f.severity());
        assertTrue(f.message().contains("2 mutating actions"));
        assertNotNull(context.getTransientData().get(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS));
    }

    @Test
    @DisplayName("Verify hierarchical steps with child sub-steps are exempted from EMPIRICAL_MULTI_ACTION")
    public void testHierarchicalStepExempted()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);

        final PlaybookStep parent = new PlaybookStep("Locate country dropdown:");
        final PlaybookStep child1 = new PlaybookStep("  - Open dropdown");
        child1.setActions(List.of(new Action("CLICK", "#dropdown", List.of(), "Open", "", true)));
        final PlaybookStep child2 = new PlaybookStep("  - Pick Germany");
        child2.setActions(List.of(new Action("CLICK", "#de", List.of(), "Pick", "", true)));

        parent.setSubSteps(List.of(child1, child2));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(parent), context);
        assertTrue(findings.isEmpty());
    }

    @Test
    @DisplayName("Verify LABEL_DIVERGENCE triggers when quoted instruction text differs from DOM text")
    public void testLabelDivergenceDetection()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);

        final PlaybookStep s = new PlaybookStep("Click the 'Add to Cart' button");
        final DomFeatureVector domVector = new DomFeatureVector("BUTTON", "In den Warenkorb", Set.of(), Map.of(), "button", "In den Warenkorb", "div", 0);
        s.setDomFeatureVector(domVector);
        s.setActions(List.of(new Action("CLICK", "button.btn-cart", List.of(), "Click cart", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(LinterCategory.LABEL_DIVERGENCE, f.category());
        assertTrue(f.message().contains("Add to Cart"));
        assertTrue(f.message().contains("In den Warenkorb"));
        assertEquals("Click the 'In den Warenkorb' button", f.suggestedRewrite());
    }

    @Test
    @DisplayName("Verify HIGH_AGENT_FRICTION triggers when agent requires > 2 turns")
    public void testHighAgentFrictionDetection()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final StepStats stats = new StepStats("Click the ambiguous item", System.currentTimeMillis());
        stats.addStandardCall(10, 10, 0);
        stats.addStandardCall(10, 10, 0);
        stats.addStandardCall(10, 10, 0);
        stats.addStandardCall(10, 10, 0);
        context.getTransientData().put("execution.stepStatsList", List.of(stats));

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("Click the ambiguous item");
        s.setActions(List.of(new Action("CLICK", "#item", List.of(), "Click item", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(LinterCategory.HIGH_AGENT_FRICTION, f.category());
        assertTrue(f.message().contains("4 agent iterations"));
    }

    @Test
    @DisplayName("Verify UNTAGGED_VISUAL_DEPENDENCY triggers when step lacked (visual) but required visual perception")
    public void testUntaggedVisualDependency()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final StepStats stats = new StepStats("The success badge is green", System.currentTimeMillis());
        stats.getContextLevels().add("VISUAL_RICH");
        context.getTransientData().put("execution.stepStatsList", List.of(stats));

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("The success badge is green");

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(LinterCategory.UNTAGGED_VISUAL_DEPENDENCY, f.category());
        assertTrue(f.suggestedRewrite().endsWith("(visual)"));
    }

    @Test
    @DisplayName("Verify REDUNDANT_VISUAL_TAG triggers when step has (visual) but resolved cleanly without visual check")
    public void testRedundantVisualTag()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final StepStats stats = new StepStats("The headline says 'Cart' (visual)", System.currentTimeMillis());
        // 0 verification calls, no VISUAL context
        context.getTransientData().put("execution.stepStatsList", List.of(stats));

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("The headline says 'Cart' (visual)");
        s.setActions(List.of(new Action("ASSERT", "h1", List.of("Cart"), "Headline is Cart", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(LinterCategory.REDUNDANT_VISUAL_TAG, f.category());
        assertEquals("The headline says 'Cart'", f.suggestedRewrite());
    }

    @Test
    @DisplayName("Verify LLM rewrite enriches deterministic findings when provider is available")
    public void testLlmRewriteEnrichment()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        System.setProperty("neodymium.ai.linter.enabled", "true");
        AiConfiguration.resetInstance();

        final MockLlmProvider mockProvider = new MockLlmProvider();
        final String llmResponse = """
            {
              "findings": [
                {
                  "stepIndex": 1,
                  "category": "EMPIRICAL_MULTI_ACTION",
                  "severity": "WARNING",
                  "message": "Step executes opening the dropdown and picking an option in a single sentence.",
                  "suggestedRewrite": "Hover over the mini cart\\nClick the 'View Cart & Checkout' button",
                  "scope": null
                }
              ]
            }
            """;
        mockProvider.addResponse(new LlmResponse(llmResponse, new TokenUsage(100, 50, 150, 0), "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.LINTER, mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);

        final PlaybookStep s = new PlaybookStep("Hover over mini cart and checkout");
        s.setActions(List.of(
            new Action("HOVER", ".mini-cart", List.of(), "Hover mini cart", "", true),
            new Action("CLICK", "#btn-checkout", List.of(), "Click checkout", "", true)
        ));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals("Hover over the mini cart\nClick the 'View Cart & Checkout' button", f.suggestedRewrite());
    }

    @Test
    @DisplayName("Verify action step like Click 'Purchase'. is never flagged as UNTAGGED_VISUAL_DEPENDENCY")
    public void testActionStepNeverTaggedVisual()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final StepStats stats = new StepStats("Click 'Purchase'.", System.currentTimeMillis());
        stats.getContextLevels().add("VISUAL_RICH");
        context.getTransientData().put("execution.stepStatsList", List.of(stats));

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("Click 'Purchase'.");
        s.setActions(List.of(new Action("CLICK", "button.purchase", List.of(), "Click Purchase", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertTrue(findings.isEmpty(), "Click 'Purchase'. must not be tagged with visual dependency");
    }

    @Test
    @DisplayName("Verify wait action step is never flagged as UNTAGGED_VISUAL_DEPENDENCY")
    public void testWaitActionStepNeverTaggedVisual()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final StepStats stats = new StepStats("Wait for text 'Order Placed'.", System.currentTimeMillis());
        stats.getContextLevels().add("VISUAL");
        context.getTransientData().put("execution.stepStatsList", List.of(stats));

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("Wait for text 'Order Placed'.");
        s.setActions(List.of(new Action("WAIT", "body", List.of(), "Wait", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertTrue(findings.isEmpty(), "Wait step must not be tagged with visual dependency");
    }

    @Test
    @DisplayName("Verify action step tagged with (visual) is flagged as REDUNDANT_VISUAL_TAG")
    public void testActionStepTaggedVisualFlaggedAsRedundant()
    {
        System.setProperty("neodymium.ai.linter.postFlight.enabled", "true");
        AiConfiguration.resetInstance();

        final AiSession session = AiSession.mock(new SessionData(), new LlmRegistry(), new ExecutionEventBus(), null);
        final ExecutionContext context = session.getExecutionContext();
        ExecutionContext.setActiveContext(context);

        final StepStats stats = new StepStats("Click 'Purchase'. (visual)", System.currentTimeMillis());
        context.getTransientData().put("execution.stepStatsList", List.of(stats));

        final PostFlightPlaybookLinter linter = new PostFlightPlaybookLinter(session);
        final PlaybookStep s = new PlaybookStep("Click 'Purchase'. (visual)");
        s.setActions(List.of(new Action("CLICK", "button.purchase", List.of(), "Click Purchase", "", true)));

        final List<PlaybookLinterFinding> findings = linter.lint(List.of(s), context);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(LinterCategory.REDUNDANT_VISUAL_TAG, f.category());
        assertEquals("Click 'Purchase'.", f.suggestedRewrite());
    }
}
