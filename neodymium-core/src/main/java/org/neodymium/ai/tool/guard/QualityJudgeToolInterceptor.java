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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Quality Judge interceptor enforcing Journey Fidelity policies and locator stability confidence scoring
 * before browser tools are dispatched to the browser.
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

        // 1. Enforce Journey Fidelity Policy (hard constraint, always evaluated)
        final InterceptionVerdict journeyVerdict = checkJourneyFidelity(call, intent);
        if (!journeyVerdict.isAllowed())
        {
            LOGGER.warn("🚨 Journey Fidelity Violation detected: {}", journeyVerdict.reason());
            return journeyVerdict;
        }

        // 2. If Quality Judge is disabled, bypass locator deliberation
        if (!this.enabled)
        {
            return InterceptionVerdict.allow("Quality Judge disabled");
        }

        // 3. Only inspect browser actions that interact with elements
        final String toolName = call.toolName();
        if (!toolName.startsWith("browser_") || "browser_take_screenshot".equals(toolName) || "browser_scroll".equals(toolName))
        {
            return InterceptionVerdict.allow("Tool " + toolName + " is exempt from locator quality judging");
        }

        // 4. Extract candidate locators from call arguments or execution context
        final List<LocatorCandidate> candidates = extractCandidates(call, context);
        if (candidates.isEmpty())
        {
            return InterceptionVerdict.allow("No candidate locators provided for scoring");
        }

        // 5. Evaluate locator candidate stability scoring
        return evaluateCandidateScoring(call, candidates);
    }

    private InterceptionVerdict checkJourneyFidelity(final ToolCall call, final SemanticIntent intent)
    {
        if (intent != null && intent.isInteraction())
        {
            if ("browser_navigate".equals(call.toolName()))
            {
                return InterceptionVerdict.reject(call.callId(), JOURNEY_FIDELITY_NAVIGATE_VIOLATION);
            }

            if ("browser_execute_script".equals(call.toolName()))
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

    private InterceptionVerdict evaluateCandidateScoring(final ToolCall call, final List<LocatorCandidate> candidates)
    {
        final LocatorCandidate top = candidates.get(0);
        final double score1 = top.getScore();

        // High confidence decisive candidate (>= 0.95) passes through immediately
        if (score1 >= 0.95)
        {
            LOGGER.debug("Quality Judge passed high-confidence locator '{}' (score: {})", top.getLocator(), score1);
            return InterceptionVerdict.allow("Decisive high-confidence locator (score: " + score1 + " >= 0.95)");
        }

        // Low confidence top candidate (< 0.85) triggers deliberation
        if (score1 < 0.85)
        {
            LOGGER.info("⚖️ Quality Judge triggered deliberation: top candidate score {} < 0.85", score1);
            return deliberateCandidates(call, candidates, "Top candidate confidence is below threshold (" + score1 + " < 0.85)");
        }

        // Multiple candidates with ambiguous score difference (< 0.15) trigger deliberation
        if (candidates.size() >= 2)
        {
            final LocatorCandidate second = candidates.get(1);
            final double diff = score1 - second.getScore();
            if (diff < 0.15)
            {
                LOGGER.info("⚖️ Quality Judge triggered deliberation: ambiguous score difference {} < 0.15 between top candidates", diff);
                return deliberateCandidates(call, candidates, "Ambiguous candidates with close scores (diff: " + diff + " < 0.15)");
            }
        }

        return InterceptionVerdict.allow("Decisive candidate winner passed (score: " + score1 + ")");
    }

    private InterceptionVerdict deliberateCandidates(
            final ToolCall call,
            final List<LocatorCandidate> candidates,
            final String deliberationReason)
    {
        // Deliberation strategy: choose candidate with highest strategy weight (DATA_AI > ID > ATTRIBUTE > CLASS)
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
            case "ACCESSIBILITY", "ARIA" -> 80;
            case "ATTRIBUTE", "NAME" -> 70;
            case "TEXT" -> 50;
            case "CLASS" -> 40;
            default -> 10;
        };
    }
}
