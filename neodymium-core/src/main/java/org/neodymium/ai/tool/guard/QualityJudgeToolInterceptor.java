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
package org.neodymium.ai.tool.guard;

import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.probe.LocatorProbeResult;
import org.neodymium.ai.executor.selenide.SelenideLocatorProber;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.prompt.QualityJudgePrompt;
import org.neodymium.ai.prompt.QualityJudgePrompt.QualityJudgeResult;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.util.LocatorImprover;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quality Judge interceptor enforcing Journey Fidelity policies, fast-path unique locator gating,
 * and pre-action LLM Quality Judge deliberation before browser tools are dispatched to the browser.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class QualityJudgeToolInterceptor implements ToolInterceptor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QualityJudgeToolInterceptor.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String JOURNEY_FIDELITY_NAVIGATE_VIOLATION =
            "Journey Fidelity Violation: Direct URL navigation is prohibited. Target must be reached via on-screen UI elements";

    public static final String JOURNEY_FIDELITY_SCRIPT_VIOLATION =
            "Journey Fidelity Violation: Direct URL mutation via script is prohibited. Target must be reached via on-screen UI elements";

    public static final String ASSERTION_MUTATION_VIOLATION =
            "Assertion Violation: Interactive mutating action is prohibited during assertion steps";

    private static final Pattern URL_MUTATION_PATTERN = Pattern.compile(
            "(?i)((window\\.)?location(\\.href|\\.assign|\\.replace)?\\s*=|history\\.(pushState|replaceState))");

    private final AiConfiguration config;

    private final boolean enabled;

    /**
     * Constructs a default QualityJudgeToolInterceptor using global {@link AiConfiguration}
     * with active locator guarding enabled.
     */
    public QualityJudgeToolInterceptor()
    {
        this(true);
    }

    /**
     * Constructs a QualityJudgeToolInterceptor with explicit enablement.
     *
     * @param enabled whether locator quality judging is enabled
     */
    public QualityJudgeToolInterceptor(final boolean enabled)
    {
        this(AiConfiguration.getInstance(), enabled);
    }

    /**
     * Constructs a QualityJudgeToolInterceptor with explicit configuration.
     *
     * @param config configuration instance
     */
    public QualityJudgeToolInterceptor(final AiConfiguration config)
    {
        this(config, true);
    }

    /**
     * Constructs a QualityJudgeToolInterceptor with explicit configuration and enablement.
     *
     * @param config configuration instance
     * @param enabled whether locator quality judging is enabled
     */
    public QualityJudgeToolInterceptor(final AiConfiguration config, final boolean enabled)
    {
        this.config = config != null ? config : AiConfiguration.getInstance();
        this.enabled = enabled;
    }

    @Override
    public InterceptionVerdict intercept(final ToolCall call, final ToolContext context, final SemanticIntent intent)
    {
        if (call == null)
        {
            return InterceptionVerdict.allow("Null tool call");
        }

        final ExecutionContext activeContext = ExecutionContext.getActiveContext();

        // 1. Enforce Journey Fidelity Policy (hard constraint, always evaluated)
        final InterceptionVerdict journeyVerdict = checkJourneyFidelity(call, intent, activeContext);
        if (!journeyVerdict.isAllowed())
        {
            LOGGER.warn("🚨 Journey Fidelity Violation detected: {}", journeyVerdict.reason());
            return journeyVerdict;
        }

        // 2. If Quality Judge is disabled via instance flag, bypass locator deliberation
        if (!this.enabled)
        {
            return InterceptionVerdict.allow("Quality Judge disabled");
        }

        // 3. Only inspect browser actions that interact with elements
        final String rawName = call.toolName();
        final String toolName = rawName.startsWith("browser_") ? rawName.substring("browser_".length()) : rawName;
        if ("screenshot".equals(toolName) || "scroll".equals(toolName)
                || "assert_count".equals(toolName) || "query_dom".equals(toolName)
                || "complete_step".equals(toolName) || "wait".equals(toolName)
                || "clear_cookies".equals(toolName) || "back".equals(toolName)
                || "forward".equals(toolName) || "refresh".equals(toolName)
                || "list_tabs".equals(toolName) || "switch_tab".equals(toolName)
                || "close_tab".equals(toolName) || "store".equals(toolName)
                || "request_context".equals(toolName))
        {
            return InterceptionVerdict.allow("Tool " + rawName + " is exempt from locator quality judging");
        }

        // 4. Extract target selector from call arguments
        final String selector = call.arguments().path("selector").asText("").trim();
        List<LocatorCandidate> candidates = extractCandidates(call, context);

        // 5. Inspect live DOM when WebDriver has started and selector is provided
        if (!selector.isEmpty() && WebDriverRunner.hasWebDriverStarted())
        {
            final WebDriver driver = WebDriverRunner.getWebDriver();
            List<WebElement> matchedElements = Collections.emptyList();
            try
            {
                matchedElements = driver.findElements(By.cssSelector(selector));
            }
            catch (final Exception e)
            {
                LOGGER.debug("Quality Judge could not query DOM for selector '{}': {}", selector, e.getMessage());
            }

            final int score = LocatorImprover.scoreLocator(selector);

            // Fast-path: single match and strong resilience score (>= 8) passes immediately in 0ms
            if (matchedElements.size() == 1 && score >= 8 && candidates.isEmpty())
            {
                LOGGER.debug("Quality Judge fast-path: decisive unique selector '{}' (score: {})", selector, score);
                return InterceptionVerdict.allow("Clean unique high-resilience selector (score: " + score + " >= 8)");
            }

            // If selector does not match any element in DOM and no candidates exist, defer to normal execution
            if (matchedElements.isEmpty() && candidates.isEmpty())
            {
                return InterceptionVerdict.allow("Element not currently matched in DOM; proceeding to execution");
            }

            // If ambiguous (multiple elements) or volatile (score < 6), generate candidate alternatives
            if (candidates.isEmpty() && !matchedElements.isEmpty() && (matchedElements.size() > 1 || score < 6))
            {
                final List<String> generated = LocatorImprover.generateCandidates(matchedElements.get(0));
                candidates = new ArrayList<>();
                candidates.add(new LocatorCandidate(selector, determineStrategy(selector), (double) score / 10.0, "Original proposed selector"));
                for (final String gen : generated)
                {
                    if (!gen.equals(selector))
                    {
                        final int genScore = LocatorImprover.scoreLocator(gen);
                        candidates.add(new LocatorCandidate(gen, determineStrategy(gen), (double) genScore / 10.0, "Generated DOM attribute candidate"));
                    }
                }
            }
        }

        if (candidates.isEmpty())
        {
            return InterceptionVerdict.allow("No candidate locators provided for scoring");
        }

        // 6. Evaluate candidate confidence scoring and trigger LLM deliberation if needed
        return evaluateCandidateScoring(call, selector, candidates, activeContext);
    }

    private InterceptionVerdict checkJourneyFidelity(
        final ToolCall call,
        final SemanticIntent intent,
        final ExecutionContext activeContext
    )
    {
        final String rawName = call.toolName();
        final String name = rawName.startsWith("browser_") ? rawName.substring("browser_".length()) : rawName;
        if (intent != null && intent.isAssertion())
        {
            if ("click".equals(name) || "fill".equals(name) || "type".equals(name) || "select".equals(name)
                    || "press_key".equals(name) || "navigate".equals(name) || "upload_file".equals(name)
                    || "drag".equals(name) || "drag_to".equals(name))
            {
                if (hasInteractiveMilestones(activeContext))
                {
                    return InterceptionVerdict.allow("Permitting interactive tool for compound step with interactive milestones");
                }
                return InterceptionVerdict.reject(call.callId(), ASSERTION_MUTATION_VIOLATION);
            }
        }

        if (intent != null && intent.isInteraction())
        {
            if ("navigate".equals(name))
            {
                return InterceptionVerdict.reject(call.callId(), JOURNEY_FIDELITY_NAVIGATE_VIOLATION);
            }

            if ("execute_script".equals(name))
            {
                final String script = call.arguments().path("script").asText("");
                if (URL_MUTATION_PATTERN.matcher(script).find())
                {
                    return InterceptionVerdict.reject(call.callId(), JOURNEY_FIDELITY_SCRIPT_VIOLATION);
                }
            }
        }
        return InterceptionVerdict.allow("Journey fidelity checks passed");
    }

    private static boolean hasInteractiveMilestones(final ExecutionContext context)
    {
        if (context == null)
        {
            return false;
        }
        final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        if (stepObj instanceof final PlaybookStep step && step.hasInteractiveSubSteps())
        {
            return true;
        }
        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);
        if (milestones != null)
        {
            for (final String ms : milestones)
            {
                if (ms != null && PlaybookStep.INTERACTIVE_ACTION_PATTERN.matcher(ms).find())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private List<LocatorCandidate> extractCandidates(final ToolCall call, final ToolContext context)
    {
        final JsonNode candidatesNode = call.arguments().path("candidates");
        if (candidatesNode.isArray() && !candidatesNode.isEmpty())
        {
            try
            {
                final List<LocatorCandidate> list = MAPPER.readValue(
                        candidatesNode.traverse(),
                        new TypeReference<List<LocatorCandidate>>() {}
                );
                return list != null ? list : Collections.emptyList();
            }
            catch (final Exception e)
            {
                LOGGER.debug("Failed to deserialize candidates from tool arguments: {}", e.getMessage());
            }
        }

        if (context != null)
        {
            final Optional<List> contextCandidates = context.getVariable("candidateLocators", List.class);
            if (contextCandidates.isPresent())
            {
                final List<?> rawList = contextCandidates.get();
                final List<LocatorCandidate> parsed = new ArrayList<>();
                for (final Object item : rawList)
                {
                    if (item instanceof final LocatorCandidate cand)
                    {
                        parsed.add(cand);
                    }
                }
                if (!parsed.isEmpty())
                {
                    return parsed;
                }
            }
        }

        return Collections.emptyList();
    }

    private InterceptionVerdict evaluateCandidateScoring(
            final ToolCall call,
            final String selector,
            final List<LocatorCandidate> candidates,
            final ExecutionContext activeContext)
    {
        final LocatorCandidate top = candidates.get(0);
        final double score1 = top.getScore();

        // High confidence decisive candidate (>= 0.95) passes through immediately
        if (score1 >= 0.95)
        {
            LOGGER.debug("Quality Judge passed high-confidence locator '{}' (score: {})", top.getLocator(), score1);
            return InterceptionVerdict.allow("Decisive high-confidence locator (score: " + score1 + " >= 0.95)");
        }

        final String judgeMode = this.config.getJudgeMode();
        final boolean isRetry = activeContext != null && Boolean.TRUE.equals(activeContext.getTransientData().get("isRetryExecution"));
        if ("ON_FAIL".equalsIgnoreCase(judgeMode) && !isRetry)
        {
            return InterceptionVerdict.allow("Quality Judge mode is ON_FAIL; skipping pre-action deliberation");
        }

        // Low confidence top candidate (< 0.85) triggers deliberation
        if (score1 < 0.85)
        {
            LOGGER.info("⚖️ Quality Judge triggered deliberation: top candidate score {} < 0.85", score1);
            return deliberate(call, selector, candidates, activeContext,
                    "Top candidate confidence is below threshold (" + score1 + " < 0.85)");
        }

        // Multiple candidates with ambiguous score difference (< 0.15) trigger deliberation
        if (candidates.size() >= 2)
        {
            final LocatorCandidate second = candidates.get(1);
            final double diff = score1 - second.getScore();
            if (diff < 0.15)
            {
                LOGGER.info("⚖️ Quality Judge triggered deliberation: ambiguous score difference {} < 0.15 between top candidates", diff);
                return deliberate(call, selector, candidates, activeContext,
                        "Ambiguous candidates with close scores (diff: " + diff + " < 0.15)");
            }
        }

        return InterceptionVerdict.allow("Decisive candidate winner passed (score: " + score1 + ")");
    }

    private InterceptionVerdict deliberate(
            final ToolCall call,
            final String selector,
            final List<LocatorCandidate> candidates,
            final ExecutionContext activeContext,
            final String deliberationReason)
    {
        final String judgeMode = this.config.getJudgeMode();
        if ("DISCUSSION".equalsIgnoreCase(judgeMode))
        {
            return deliberateInteractiveDiscussion(call, selector, candidates, activeContext, deliberationReason);
        }
        return deliberateWithLlmJudgeSingleShot(call, selector, candidates, activeContext, deliberationReason);
    }

    private InterceptionVerdict deliberateInteractiveDiscussion(
            final ToolCall call,
            final String selector,
            final List<LocatorCandidate> candidates,
            final ExecutionContext activeContext,
            final String deliberationReason)
    {
        final AiSession session = activeContext != null
                ? (AiSession) activeContext.getTransientData().get(ExecutionContext.KEY_SESSION)
                : null;

        if (this.config.isJudgeEnabled() && session != null && session.getLlmRegistry() != null)
        {
            try
            {
                final String instruction = (String) activeContext.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_INSTRUCTION, "");
                final Object stateObj = activeContext.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
                final String domContext = stateObj instanceof final SutState sutState ? sutState.getTextContent() : "";

                final int probeDepth = this.config.getJudgeDiscussionProbeDepth();
                List<String> currentCandidateStrings = new ArrayList<>();
                for (final LocatorCandidate c : candidates)
                {
                    final String loc = c.getLocator();
                    if (loc != null && !loc.isBlank() && !currentCandidateStrings.contains(loc))
                    {
                        currentCandidateStrings.add(loc);
                    }
                }
                if (currentCandidateStrings.isEmpty() && !selector.isBlank())
                {
                    currentCandidateStrings.add(selector);
                }

                // Initial probe
                List<LocatorProbeResult> probeResults = probeCandidateLocators(currentCandidateStrings, activeContext, probeDepth);

                // Fast-Path: if Candidate 1 has strong confidence (>= 0.85) and probe confirms unique visible match
                if (this.config.isJudgeDiscussionFastPathEnabled() && !probeResults.isEmpty())
                {
                    final LocatorProbeResult firstProbe = probeResults.get(0);
                    final LocatorCandidate firstCandidate = candidates.get(0);
                    if (firstCandidate.getScore() >= 0.85
                            && firstProbe.isUnique()
                            && !firstProbe.getMatches().isEmpty()
                            && firstProbe.getMatches().get(0).isVisible())
                    {
                        LOGGER.info("⚡ [Quality Judge Fast-Path] Decisive unique locator '{}' confirmed in live DOM (0 extra LLM calls)", firstProbe.getCandidateLocator());
                        return InterceptionVerdict.allow("Fast-path auto-approved unique locator: " + firstProbe.getCandidateLocator());
                    }
                }

                final int maxTurns = Math.max(1, this.config.getJudgeDiscussionMaxTurns());
                final List<String> history = new ArrayList<>();
                String lastProposal = selector.isEmpty() && !currentCandidateStrings.isEmpty() ? currentCandidateStrings.get(0) : selector;
                final QualityJudgePrompt judgePrompt = new QualityJudgePrompt();
                final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

                for (int turn = 1; turn <= maxTurns; turn++)
                {
                    if (turn > 1)
                    {
                        probeResults = probeCandidateLocators(currentCandidateStrings, activeContext, probeDepth);
                    }

                    LOGGER.info("⚖️ [Quality Judge Discussion] Turn {}/{} probing candidates: {}", turn, maxTurns, currentCandidateStrings);

                    final LlmRequest request = judgePrompt.compileDiscussionRequest(
                            instruction, probeResults, history, turn, maxTurns, domContext, this.config);

                    if (session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new LlmRequestSentEvent(request, "JUDGE_DISCUSSION"));
                    }
                    final long startTime = System.currentTimeMillis();
                    final LlmResponse response = provider.chat(request);
                    final long durationMs = System.currentTimeMillis() - startTime;

                    if (session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new LlmResponseReceivedEvent(request, response, durationMs, "JUDGE_DISCUSSION"));
                    }

                    recordMetrics(activeContext, response);

                    if (response == null || response.content() == null || response.content().isBlank())
                    {
                        LOGGER.warn("Empty response from Judge in turn {}/{}", turn, maxTurns);
                        break;
                    }

                    final QualityJudgeResult judgeResult = judgePrompt.parseResponse(response.content());
                    final String status = judgeResult.getStatus();
                    LOGGER.info("⚖️ [Quality Judge] Turn {}/{} Status: {} | Chosen: '{}' | Refined: '{}' | Reasoning: {}",
                            turn, maxTurns, status, judgeResult.getChosenLocator(), judgeResult.getRefinedProposal(), judgeResult.getReasoning());

                    if ("APPROVED".equalsIgnoreCase(status))
                    {
                        final String winningLocator = !judgeResult.getChosenLocator().isBlank()
                                ? judgeResult.getChosenLocator()
                                : currentCandidateStrings.get(0);
                        LOGGER.info("⚖️ [Quality Judge Consensus] APPROVED in Turn {}/{}: '{}' ({})", turn, maxTurns, winningLocator, judgeResult.getReasoning());
                        if (!winningLocator.equals(selector))
                        {
                            final ObjectNode newArgs = call.arguments().deepCopy();
                            newArgs.put("selector", winningLocator);
                            final ToolCall adjusted = new ToolCall(call.callId(), call.toolName(), newArgs);
                            return InterceptionVerdict.deliberated(adjusted, "LLM Quality Judge approved locator: " + winningLocator + " (" + judgeResult.getReasoning() + ")");
                        }
                        return InterceptionVerdict.allow("LLM Quality Judge confirmed locator: " + selector + " (" + judgeResult.getReasoning() + ")");
                    }
                    else if ("REFINED".equalsIgnoreCase(status))
                    {
                        final String chosen = judgeResult.getChosenLocator();
                        if (chosen != null && !chosen.isBlank())
                        {
                            lastProposal = chosen;
                            final Optional<LocatorProbeResult> matchedProbe = probeResults.stream()
                                    .filter(pr -> pr.getCandidateLocator().equals(chosen))
                                    .findFirst();
                            final boolean probingSupported = probeResults.stream().anyMatch(LocatorProbeResult::isSupported);
                            if (!probingSupported || (matchedProbe.isPresent() && matchedProbe.get().isUnique()))
                            {
                                LOGGER.info("⚖️ [Quality Judge Consensus] REFINED to verified candidate in Turn {}/{}: '{}'", turn, maxTurns, chosen);
                                final ObjectNode newArgs = call.arguments().deepCopy();
                                newArgs.put("selector", chosen);
                                final ToolCall adjusted = new ToolCall(call.callId(), call.toolName(), newArgs);
                                return InterceptionVerdict.deliberated(adjusted, "LLM Quality Judge selected verified candidate: " + chosen + " (" + judgeResult.getReasoning() + ")");
                            }

                            if (turn < maxTurns)
                            {
                                history.add(String.format("Turn %d Refinement: %s -> Chosen candidate: '%s'", turn, judgeResult.getReasoning(), chosen));
                                currentCandidateStrings = new ArrayList<>(List.of(chosen));
                                continue;
                            }
                        }
                    }
                    else if ("NEED_REFINEMENT".equalsIgnoreCase(status))
                    {
                        final String refined = judgeResult.getRefinedProposal();
                        if (refined != null && !refined.isBlank())
                        {
                            lastProposal = refined;
                            history.add(String.format("Turn %d Critique: %s -> Refined Proposal: '%s'", turn, judgeResult.getReasoning(), refined));
                            if (turn < maxTurns)
                            {
                                currentCandidateStrings = new ArrayList<>(List.of(refined));
                                continue;
                            }
                        }
                        else
                        {
                            history.add(String.format("Turn %d Critique: %s", turn, judgeResult.getReasoning()));
                        }
                    }
                }

                // Exhaustion Fallback: limit turns to X and proceed with last proposal
                LOGGER.warn("⚠️ [Quality Judge] Reached max turns ({}) without explicit consensus. Proceeding with last proposal: '{}'", maxTurns, lastProposal);
                if (lastProposal != null && !lastProposal.isBlank() && !lastProposal.equals(selector))
                {
                    final ObjectNode newArgs = call.arguments().deepCopy();
                    newArgs.put("selector", lastProposal);
                    final ToolCall adjusted = new ToolCall(call.callId(), call.toolName(), newArgs);
                    return InterceptionVerdict.deliberated(adjusted, "Max discussion turns (" + maxTurns + ") reached; proceeded with last proposal: " + lastProposal);
                }
                return InterceptionVerdict.allow("Max discussion turns (" + maxTurns + ") reached; proceeded with locator: " + selector);
            }
            catch (final Exception e)
            {
                LOGGER.warn("⚠️ Quality Judge LLM discussion deliberation encountered an error: {}. Falling back to candidate heuristic.", e.getMessage());
            }
        }

        return deliberateCandidatesHeuristically(call, candidates, deliberationReason);
    }

    private List<LocatorProbeResult> probeCandidateLocators(
            final List<String> candidateLocators,
            final ExecutionContext activeContext,
            final int maxDepth)
    {
        final Object executorObj = activeContext != null ? activeContext.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR) : null;
        if (executorObj instanceof final TargetExecutor targetExecutor && targetExecutor.supportsLocatorProbing())
        {
            return targetExecutor.probeLocators(candidateLocators, maxDepth);
        }
        if (activeContext != null && activeContext.getTransientData().get(ExecutionContext.KEY_SESSION) instanceof final AiSession session
                && session.getTargetExecutor() != null && session.getTargetExecutor().supportsLocatorProbing())
        {
            return session.getTargetExecutor().probeLocators(candidateLocators, maxDepth);
        }
        if (WebDriverRunner.hasWebDriverStarted())
        {
            return SelenideLocatorProber.probe(WebDriverRunner.getWebDriver(), candidateLocators, maxDepth);
        }
        return candidateLocators.stream().map(LocatorProbeResult::unsupported).toList();
    }

    private static void recordMetrics(final ExecutionContext activeContext, final LlmResponse response)
    {
        if (activeContext == null)
        {
            return;
        }
        final Integer totalCalls = (Integer) activeContext.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
        activeContext.getTransientData().put(ExecutionContext.KEY_TOTAL_LLM_CALLS, totalCalls + 1);
        final Integer judgeCalls = (Integer) activeContext.getTransientData().getOrDefault(ExecutionContext.KEY_JUDGE_CALL_COUNT, 0);
        activeContext.getTransientData().put(ExecutionContext.KEY_JUDGE_CALL_COUNT, judgeCalls + 1);

        final TokenUsage newUsage = response != null ? response.tokenUsage() : null;
        if (newUsage != null)
        {
            final TokenUsage existing = (TokenUsage) activeContext.getTransientData().get(ExecutionContext.KEY_JUDGE_TOKEN_USAGE);
            if (existing != null)
            {
                activeContext.getTransientData().put(ExecutionContext.KEY_JUDGE_TOKEN_USAGE, new TokenUsage(
                        existing.inputTokenCount() + newUsage.inputTokenCount(),
                        existing.outputTokenCount() + newUsage.outputTokenCount(),
                        existing.totalTokenCount() + newUsage.totalTokenCount(),
                        existing.cachedTokenCount() + newUsage.cachedTokenCount()
                ));
            }
            else
            {
                activeContext.getTransientData().put(ExecutionContext.KEY_JUDGE_TOKEN_USAGE, newUsage);
            }
        }
    }

    private InterceptionVerdict deliberateWithLlmJudgeSingleShot(
            final ToolCall call,
            final String selector,
            final List<LocatorCandidate> candidates,
            final ExecutionContext activeContext,
            final String deliberationReason)
    {
        final AiSession session = activeContext != null
                ? (AiSession) activeContext.getTransientData().get(ExecutionContext.KEY_SESSION)
                : null;

        if (this.config.isJudgeEnabled() && session != null && session.getLlmRegistry() != null)
        {
            try
            {
                final String instruction = (String) activeContext.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_INSTRUCTION, "");
                final Object stateObj = activeContext.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
                final String domContext = stateObj instanceof final SutState sutState ? sutState.getTextContent() : "";

                final Action proposedAction = new Action(call.toolName(), selector.isEmpty() ? candidates.get(0).getLocator() : selector, "");
                proposedAction.setCandidateLocators(candidates);

                final QualityJudgePrompt judgePrompt = new QualityJudgePrompt();
                final LlmRequest request = judgePrompt.compileRequest(instruction, domContext, proposedAction, this.config);

                final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
                LOGGER.debug("💬 [Quality Judge] Deliberating proposed locator '{}' with LLM provider '{}'",
                        proposedAction.getTarget(), provider.getClass().getSimpleName());

                if (session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new LlmRequestSentEvent(request, "JUDGE"));
                }
                final long startTime = System.currentTimeMillis();
                final LlmResponse response = provider.chat(request);
                final long durationMs = System.currentTimeMillis() - startTime;

                if (session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new LlmResponseReceivedEvent(request, response, durationMs, "JUDGE"));
                }

                recordMetrics(activeContext, response);

                if (response != null && response.content() != null)
                {
                    final QualityJudgeResult judgeResult = judgePrompt.parseResponse(response.content());
                    LOGGER.info("⚖️ Quality Judge Judgment: {} | Chosen Locator: '{}' | Reasoning: {}",
                            judgeResult.getJudgment(), judgeResult.getChosenLocator(), judgeResult.getReasoning());

                    final String chosen = judgeResult.getChosenLocator();
                    if (chosen != null && !chosen.isBlank() && !chosen.equals(selector))
                    {
                        final ObjectNode newArgs = call.arguments().deepCopy();
                        newArgs.put("selector", chosen);
                        final ToolCall adjusted = new ToolCall(call.callId(), call.toolName(), newArgs);
                        return InterceptionVerdict.deliberated(
                                adjusted,
                                "LLM Quality Judge selected refined locator: " + chosen + " (" + judgeResult.getReasoning() + ")"
                        );
                    }
                    return InterceptionVerdict.allow(
                            "LLM Quality Judge confirmed locator: " + selector + " (" + judgeResult.getReasoning() + ")"
                    );
                }
            }
            catch (final Exception e)
            {
                LOGGER.warn("⚠️ Quality Judge LLM deliberation encountered an error: {}. Falling back to candidate heuristic.", e.getMessage());
            }
        }

        // Fallback: heuristic candidate deliberation
        return deliberateCandidatesHeuristically(call, candidates, deliberationReason);
    }

    private InterceptionVerdict deliberateCandidatesHeuristically(
            final ToolCall call,
            final List<LocatorCandidate> candidates,
            final String deliberationReason)
    {
        LocatorCandidate bestCandidate = candidates.get(0);
        int bestWeight = getStrategyWeight(bestCandidate.getStrategy());

        for (int i = 1; i < candidates.size(); i++)
        {
            final LocatorCandidate cand = candidates.get(i);
            final int weight = getStrategyWeight(cand.getStrategy());
            if (weight > bestWeight || (weight == bestWeight && cand.getScore() > bestCandidate.getScore()))
            {
                bestCandidate = cand;
                bestWeight = weight;
            }
        }

        final String chosenLocator = bestCandidate.getLocator();
        final JsonNode args = call.arguments();
        final String originalSelector = args.path("selector").asText("");

        if (!chosenLocator.isEmpty() && !chosenLocator.equals(originalSelector))
        {
            final ObjectNode newArgs = args.deepCopy();
            newArgs.put("selector", chosenLocator);
            final ToolCall adjustedCall = new ToolCall(call.callId(), call.toolName(), newArgs);
            return InterceptionVerdict.deliberated(
                    adjustedCall,
                    deliberationReason + " -> Resolved to most resilient candidate: " + chosenLocator
            );
        }

        return InterceptionVerdict.deliberated(
                call,
                deliberationReason + " -> Confirmed candidate: " + bestCandidate.getLocator()
        );
    }

    private static String determineStrategy(final String locator)
    {
        if (locator == null)
        {
            return "UNKNOWN";
        }
        final String trimmed = locator.trim();
        if (trimmed.startsWith("#") || trimmed.contains("#"))
        {
            return "ID";
        }
        if (trimmed.contains("data-testid") || trimmed.contains("data-test"))
        {
            return "TEST_ID";
        }
        if (trimmed.contains("aria-label"))
        {
            return "ARIA";
        }
        if (trimmed.contains("[name="))
        {
            return "NAME";
        }
        if (trimmed.contains("placeholder"))
        {
            return "PLACEHOLDER";
        }
        if (trimmed.startsWith("."))
        {
            return "CLASS";
        }
        if (trimmed.contains("data-ai"))
        {
            return "DATA_AI";
        }
        return "ATTRIBUTE";
    }

    private static int getStrategyWeight(final String strategy)
    {
        if (strategy == null)
        {
            return 0;
        }
        return switch (strategy.toUpperCase())
        {
            case "DATA_AI", "AI_ID" -> 100;
            case "ID" -> 90;
            case "TEST_ID", "DATA_TESTID", "DATA-TESTID" -> 85;
            case "ACCESSIBILITY", "ARIA" -> 80;
            case "ATTRIBUTE", "NAME" -> 70;
            case "PLACEHOLDER" -> 60;
            case "TEXT" -> 50;
            case "CLASS" -> 40;
            default -> 10;
        };
    }
}
