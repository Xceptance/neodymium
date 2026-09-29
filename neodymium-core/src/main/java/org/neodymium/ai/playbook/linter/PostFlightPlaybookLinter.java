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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.ReasoningEffort;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.prompt.PostFlightLinterPrompt;
import org.neodymium.ai.prompt.PostFlightLinterPrompt.StepTelemetryInfo;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.LlmLoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service executing empirical post-flight analysis on completed playbook steps.
 * Evaluates real-time execution telemetry (executed tool calls, mutating action counts,
 * turn thrashing, target element DOM text, and visual fallbacks) against playbook instructions,
 * producing grounded, actionable advisory findings.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PostFlightPlaybookLinter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PostFlightPlaybookLinter.class);

    private static final Pattern QUOTED_PATTERN = Pattern.compile("['\"„“]([^'\"„“]{2,})['\"„“]");

    private static final Pattern ACTION_PREFIX_PATTERN = Pattern.compile(
        "^(?i)(click|press|tap|type|enter|fill|select|choose|pick|hover|scroll|open|navigate|go to|visit|submit|upload|drag|drop|clear|wait for text|wait|pause|capture|store|increase|decrease)\\b"
    );

    private static final Pattern VISUAL_KEYWORDS_PATTERN = Pattern.compile(
        "(?i)\\b(color|colour|green|red|blue|yellow|orange|purple|white|black|gray|grey|checkmark|badge|logo|icon|avatar|image|graphic|layout|align|aligned|alignment|centered|visually|appearance|font|bold|italic|border|shadow|underlined|contrast|left of|right of|above|below|next to|in the middle|on the left|on the right|at the top|at the bottom|side by side)\\b"
    );

    private final AiSession session;

    /**
     * Constructs a PostFlightPlaybookLinter bound to the active AI session.
     *
     * @param session the active AI session
     */
    public PostFlightPlaybookLinter(final AiSession session)
    {
        this.session = session;
    }

    /**
     * Runs post-flight empirical analysis on the executed scenario steps.
     *
     * @param steps the list of playbook steps
     * @param context active execution context
     * @return list of empirical advisory findings, or empty list if disabled or no issues found
     */
    public List<PlaybookLinterFinding> lint(final List<PlaybookStep> steps, final ExecutionContext context)
    {
        if (steps == null || steps.isEmpty())
        {
            return Collections.emptyList();
        }

        final ExecutionMode mode = this.session != null ? this.session.getExecutionMode() : null;
        if (mode != null && !mode.isLive())
        {
            LOGGER.debug("Empirical post-flight playbook linter is bypassed in replay mode ({}).", mode);
            return Collections.emptyList();
        }

        final AiConfiguration config = AiConfiguration.getInstance();
        boolean enabled = config.isPostFlightLinterEnabled();

        if (this.session != null && this.session.data() != null)
        {
            final Object dynamicVal = this.session.data().get("neodymium.ai.linter.postFlight.enabled");
            if (dynamicVal != null)
            {
                enabled = Boolean.parseBoolean(String.valueOf(dynamicVal).trim());
            }
            final Object dynamicShort = this.session.data().get("neodymium.ai.postflight.linter.enabled");
            if (dynamicShort != null)
            {
                enabled = Boolean.parseBoolean(String.valueOf(dynamicShort).trim());
            }
        }

        if (context != null && context.getTransientData() != null)
        {
            final Object ctxVal = context.getTransientData().get("neodymium.ai.linter.postFlight.enabled");
            if (ctxVal != null)
            {
                enabled = Boolean.parseBoolean(String.valueOf(ctxVal).trim());
            }
        }

        if (!enabled)
        {
            LOGGER.debug("Empirical post-flight playbook linter is disabled.");
            return Collections.emptyList();
        }

        final long startTime = System.currentTimeMillis();

        try
        {
            @SuppressWarnings("unchecked")
            final List<StepStats> stepStatsList = context != null
                ? (List<StepStats>) context.getTransientData().get("execution.stepStatsList")
                : Collections.emptyList();

            final List<StepTelemetryInfo> frictionSteps = new ArrayList<>();
            final List<PlaybookLinterFinding> deterministicFindings = new ArrayList<>();

            for (int i = 0; i < steps.size(); i++)
            {
                final PlaybookStep pbStep = steps.get(i);
                final StepStats stats = findStatsForStep(pbStep, stepStatsList, i);

                final int stepIndex = i + 1;
                final int lineNumber = pbStep.getLineNumber();
                final String sourceFile = pbStep.getSourceFile();
                final String rawInstruction = pbStep.getInstruction() != null ? pbStep.getInstruction() : "";
                String resolvedInstruction = rawInstruction;

                if (context != null && context.getSessionData() != null && !rawInstruction.isBlank())
                {
                    try
                    {
                        resolvedInstruction = context.getSessionData().resolveAvailableVariables(rawInstruction);
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                final List<Action> actions = pbStep.getActions();
                final List<Action> mutatingActions = filterMutatingActions(actions);
                final boolean isHierarchical = pbStep.hasSubSteps() || pbStep.getParent() != null;
                final int agentTurns = stats != null ? stats.getStandardCalls() : 0;
                final long durationMs = stats != null ? stats.getDurationMs() : 0;
                final String status = pbStep.getStatus() != null ? pbStep.getStatus().name() : "UNKNOWN";

                final DomFeatureVector domVector = pbStep.getDomFeatureVector();
                final String observedDomText = extractInteractedText(mutatingActions, domVector);

                final List<String> actionSummaries = new ArrayList<>();
                for (final Action act : actions)
                {
                    actionSummaries.add(act.getType() + " on '" + act.getTarget() + "'"
                        + (act.getDescription() != null && !act.getDescription().isBlank() ? " (" + act.getDescription() + ")" : ""));
                }

                // Rule 1: EMPIRICAL_MULTI_ACTION (Flat step executed multiple mutating interactive actions)
                if (!isHierarchical && mutatingActions.size() > 1)
                {
                    final String reason = "Step executed " + mutatingActions.size() + " mutating actions in a single flat step.";
                    frictionSteps.add(new StepTelemetryInfo(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        status,
                        agentTurns,
                        durationMs,
                        actionSummaries,
                        observedDomText,
                        LinterCategory.EMPIRICAL_MULTI_ACTION,
                        reason
                    ));

                    deterministicFindings.add(new PlaybookLinterFinding(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        LinterCategory.EMPIRICAL_MULTI_ACTION,
                        LinterSeverity.WARNING,
                        reason,
                        generateDeterministicSplit(mutatingActions),
                        null
                    ));
                    continue;
                }

                // Rule 2: LABEL_DIVERGENCE (Instruction quoted text diverged from interacted element's accessible name / text)
                if (observedDomText != null && !observedDomText.isBlank())
                {
                    final String quotedText = extractQuotedText(resolvedInstruction);
                    if (quotedText != null && !quotedText.isBlank())
                    {
                        final String normQuoted = quotedText.toLowerCase(Locale.ROOT).trim();
                        final String normObserved = observedDomText.toLowerCase(Locale.ROOT).trim();

                        if (!normObserved.contains(normQuoted) && !normQuoted.contains(normObserved))
                        {
                            final String reason = "Instruction references '" + quotedText + "', but interacted DOM element text was '" + observedDomText + "'.";
                            frictionSteps.add(new StepTelemetryInfo(
                                stepIndex,
                                lineNumber,
                                sourceFile,
                                rawInstruction,
                                resolvedInstruction,
                                status,
                                agentTurns,
                                durationMs,
                                actionSummaries,
                                observedDomText,
                                LinterCategory.LABEL_DIVERGENCE,
                                reason
                            ));

                            final String suggested = rawInstruction.replace(quotedText, observedDomText);
                            deterministicFindings.add(new PlaybookLinterFinding(
                                stepIndex,
                                lineNumber,
                                sourceFile,
                                rawInstruction,
                                resolvedInstruction,
                                LinterCategory.LABEL_DIVERGENCE,
                                LinterSeverity.INFO,
                                reason,
                                suggested,
                                null
                            ));
                            continue;
                        }
                    }
                }

                // Rule 3: HIGH_AGENT_FRICTION (Excessive reasoning turns or thrashing)
                if (agentTurns > 2)
                {
                    final String reason = "Step required " + agentTurns + " agent iterations to resolve.";
                    frictionSteps.add(new StepTelemetryInfo(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        status,
                        agentTurns,
                        durationMs,
                        actionSummaries,
                        observedDomText,
                        LinterCategory.HIGH_AGENT_FRICTION,
                        reason
                    ));

                    deterministicFindings.add(new PlaybookLinterFinding(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        LinterCategory.HIGH_AGENT_FRICTION,
                        LinterSeverity.WARNING,
                        reason,
                        "",
                        null
                    ));
                    continue;
                }

                // Rule 4: UNTAGGED_VISUAL_DEPENDENCY (Visual or layout assertion lacking (visual) tag)
                final boolean isVisualTagged = pbStep.isVisualStep();
                final boolean isAction = isActionStep(mutatingActions, resolvedInstruction);
                final boolean hasVisualKeywords = VISUAL_KEYWORDS_PATTERN.matcher(resolvedInstruction).find();
                final boolean usedVisualEscalation = stats != null && stats.getContextLevels().contains("VISUAL_RICH");
                final boolean hadVerificationCall = stats != null && stats.getVerificationCalls() > 0;

                if (!isVisualTagged && !isAction && (hasVisualKeywords || usedVisualEscalation || hadVerificationCall))
                {
                    final String reason = "Step asserts visual or layout attributes but lacks the (visual) tag.";
                    frictionSteps.add(new StepTelemetryInfo(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        status,
                        agentTurns,
                        durationMs,
                        actionSummaries,
                        observedDomText,
                        LinterCategory.UNTAGGED_VISUAL_DEPENDENCY,
                        reason
                    ));

                    deterministicFindings.add(new PlaybookLinterFinding(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        LinterCategory.UNTAGGED_VISUAL_DEPENDENCY,
                        LinterSeverity.INFO,
                        reason,
                        rawInstruction + " (visual)",
                        "VIEWPORT"
                    ));
                    continue;
                }

                // Rule 5: REDUNDANT_VISUAL_TAG (Tagged visual but is an action or resolved cleanly without visual check)
                final boolean hasVisualContext = stats != null && stats.getContextLevels().stream()
                    .anyMatch(level -> level != null && level.startsWith("VISUAL"));
                final boolean hasVisualHash = (pbStep.getScreenshotHash() != null && !pbStep.getScreenshotHash().isBlank())
                    || (actions != null && actions.stream().anyMatch(a -> a != null && a.getStepScreenshotHash() != null && !a.getStepScreenshotHash().isBlank()));
                final boolean hadVisualVerification = hadVerificationCall || hasVisualContext || hasVisualHash;

                if (isVisualTagged && (isAction || !hadVisualVerification))
                {
                    final String reason = isAction
                        ? "Step is an interactive action and does not perform visual verification; (visual) tag is redundant."
                        : "Step is tagged (visual) but was fully resolved via standard DOM elements without visual comparison.";
                    deterministicFindings.add(new PlaybookLinterFinding(
                        stepIndex,
                        lineNumber,
                        sourceFile,
                        rawInstruction,
                        resolvedInstruction,
                        LinterCategory.REDUNDANT_VISUAL_TAG,
                        LinterSeverity.INFO,
                        reason,
                        rawInstruction.replaceAll("(?i)\\s*\\(visual(:\\s*[^)]+)?\\)", "").trim(),
                        null
                    ));
                }
            }

            if (deterministicFindings.isEmpty())
            {
                LOGGER.info("🔍 Empirical post-flight playbook linter completed in {} ms: 0 friction finding(s) detected.",
                            System.currentTimeMillis() - startTime);
                if (context != null)
                {
                    context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS, Collections.emptyList());
                }
                return Collections.emptyList();
            }

            // Tier 2: Refine rewrites with LLM if available and friction steps exist
            List<PlaybookLinterFinding> finalFindings = deterministicFindings;

            LlmProvider provider = null;
            if (this.session != null && this.session.getLlmRegistry() != null)
            {
                try
                {
                    provider = this.session.getLlmRegistry().getProvider(LlmCapability.LINTER);
                }
                catch (final Exception ignored)
                {
                    // No specialized LINTER or default provider registered
                }
            }

            final boolean allowMock = "true".equalsIgnoreCase(System.getProperty("neodymium.ai.linter.enabled"))
                || (this.session != null && this.session.data() != null && "true".equals(String.valueOf(this.session.data().get("neodymium.ai.linter.enabled"))));

            if (provider != null && (!frictionSteps.isEmpty()) && (!(provider instanceof MockLlmProvider) || allowMock))
            {
                try
                {
                    String scenarioDesc = (String) context.getTransientData().get(ExecutionContext.KEY_SCENARIO_DESCRIPTION);
                    if (scenarioDesc == null || scenarioDesc.isBlank())
                    {
                        final Playbook playbook = (Playbook) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
                        if (playbook != null && playbook.getDescription() != null && !playbook.getDescription().isBlank())
                        {
                            scenarioDesc = playbook.getDescription();
                        }
                    }

                    final PostFlightLinterPrompt prompt = new PostFlightLinterPrompt(scenarioDesc, frictionSteps);
                    final String sysMsg = prompt.compileSystemMessage(context);
                    final String userMsg = prompt.compileUserMessage(context);

                    final double temperature = config.getTemperature("linter");
                    final int timeoutSeconds = config.getTimeoutSeconds("linter");
                    final ReasoningEffort reasoningEffort = config.getLinterReasoningEffort();

                    final LlmRequest request = new LlmRequest(
                        sysMsg,
                        userMsg,
                        Collections.emptyList(),
                        ResponseSchema.LINTER,
                        temperature,
                        timeoutSeconds,
                        reasoningEffort
                    );

                    if (this.session.getEventBus() != null)
                    {
                        this.session.getEventBus().dispatch(new LlmRequestSentEvent(request, "POST_FLIGHT_LINTER"));
                    }

                    final long llmStart = System.currentTimeMillis();
                    final LlmResponse response = provider.chat(request);
                    final long llmDuration = System.currentTimeMillis() - llmStart;

                    if (LOGGER.isDebugEnabled() && response != null && response.content() != null)
                    {
                        LOGGER.debug("   🔍 [Post-Flight Linter Response]:\n{}", LlmLoggingUtils.formatJsonForLogging(response.content()));
                    }

                    if (response != null)
                    {
                        final TokenUsage usage = response.tokenUsage();
                        if (usage != null)
                        {
                            final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_POST_FLIGHT_LINTER_TOKEN_USAGE);
                            final TokenUsage updated = existing != null
                                ? new TokenUsage(
                                    existing.inputTokenCount() + usage.inputTokenCount(),
                                    existing.outputTokenCount() + usage.outputTokenCount(),
                                    existing.totalTokenCount() + usage.totalTokenCount(),
                                    existing.cachedTokenCount() + usage.cachedTokenCount())
                                : usage;
                            context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_TOKEN_USAGE, updated);
                        }

                        final Integer existingCalls = (Integer) context.getTransientData().get(ExecutionContext.KEY_POST_FLIGHT_LINTER_CALL_COUNT);
                        context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_CALL_COUNT, (existingCalls != null ? existingCalls : 0) + 1);

                        if (this.session.getEventBus() != null)
                        {
                            this.session.getEventBus().dispatch(new LlmResponseReceivedEvent(request, response, llmDuration, "POST_FLIGHT_LINTER"));
                        }

                        final List<PlaybookLinterFinding> llmFindings = prompt.parseResponse(response.content(), context);
                        if (!llmFindings.isEmpty())
                        {
                            finalFindings = mergeFindings(deterministicFindings, llmFindings);
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("⚠️ Post-flight LLM rewrite generation encountered an issue; falling back to deterministic findings: {}", e.getMessage());
                }
            }

            final long totalDuration = System.currentTimeMillis() - startTime;
            LOGGER.info("🔍 Empirical post-flight playbook linter completed in {} ms: {} finding(s) detected.",
                        totalDuration, finalFindings.size());

            for (final PlaybookLinterFinding f : finalFindings)
            {
                LOGGER.info("   ⚠️  [Step #{}{}] [{} / {}] {}",
                            f.stepIndex(),
                            f.lineNumber() > 0 ? ", line " + f.lineNumber() : "",
                            f.category(),
                            f.severity(),
                            f.message());
                if (f.suggestedRewrite() != null && !f.suggestedRewrite().isBlank())
                {
                    LOGGER.info("      💡 Suggested Rewrite: {}", f.suggestedRewrite().replace("\n", "\n         "));
                }
            }

            if (context != null)
            {
                context.getTransientData().put(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS, finalFindings);
            }

            return finalFindings;
        }
        catch (final Throwable t)
        {
            LOGGER.warn("⚠️ Empirical post-flight playbook linter encountered an error and was skipped gracefully: {}", t.getMessage(), t);
            return Collections.emptyList();
        }
    }

    private static List<PlaybookLinterFinding> mergeFindings(
        final List<PlaybookLinterFinding> deterministic,
        final List<PlaybookLinterFinding> llmFindings)
    {
        final List<PlaybookLinterFinding> merged = new ArrayList<>();

        for (final PlaybookLinterFinding det : deterministic)
        {
            PlaybookLinterFinding matchedLlm = null;
            for (final PlaybookLinterFinding llm : llmFindings)
            {
                if (llm.stepIndex() == det.stepIndex())
                {
                    matchedLlm = llm;
                    break;
                }
            }

            if (matchedLlm != null && matchedLlm.suggestedRewrite() != null && !matchedLlm.suggestedRewrite().isBlank())
            {
                merged.add(new PlaybookLinterFinding(
                    det.stepIndex(),
                    det.lineNumber(),
                    det.sourceFile(),
                    det.rawInstruction(),
                    det.resolvedInstruction(),
                    matchedLlm.category() != null ? matchedLlm.category() : det.category(),
                    det.severity(),
                    matchedLlm.message() != null && !matchedLlm.message().isBlank() ? matchedLlm.message() : det.message(),
                    matchedLlm.suggestedRewrite(),
                    matchedLlm.scope() != null ? matchedLlm.scope() : det.scope()
                ));
            }
            else
            {
                merged.add(det);
            }
        }

        return merged;
    }

    private static StepStats findStatsForStep(final PlaybookStep step, final List<StepStats> statsList, final int index)
    {
        if (statsList == null || statsList.isEmpty())
        {
            return null;
        }

        for (final StepStats stats : statsList)
        {
            if (stats.getInstruction() != null && stats.getInstruction().equals(step.getInstruction()))
            {
                return stats;
            }
        }

        if (index >= 0 && index < statsList.size())
        {
            return statsList.get(index);
        }

        return null;
    }

    private static List<Action> filterMutatingActions(final List<Action> actions)
    {
        if (actions == null || actions.isEmpty())
        {
            return Collections.emptyList();
        }

        final List<Action> mutating = new ArrayList<>();
        for (final Action a : actions)
        {
            final String type = a.getType() != null ? a.getType().toUpperCase(Locale.ROOT) : "";
            if ("CLICK".equals(type) || "TYPE".equals(type) || "FILL".equals(type)
                || "SELECT".equals(type) || "PRESS_KEY".equals(type) || "NAVIGATE".equals(type)
                || "UPLOAD_FILE".equals(type) || "DRAG".equals(type) || "DRAG_TO".equals(type)
                || "HOVER".equals(type))
            {
                mutating.add(a);
            }
        }
        return mutating;
    }

    private static boolean isActionStep(final List<Action> mutatingActions, final String instruction)
    {
        if (mutatingActions != null && !mutatingActions.isEmpty())
        {
            return true;
        }
        if (instruction == null || instruction.isBlank())
        {
            return false;
        }
        return ACTION_PREFIX_PATTERN.matcher(instruction.trim()).find();
    }

    private static String extractInteractedText(final List<Action> mutatingActions, final DomFeatureVector stepVector)
    {
        for (final Action act : mutatingActions)
        {
            if (act.getDomFeatureVector() != null && act.getDomFeatureVector().getText() != null
                && !act.getDomFeatureVector().getText().isBlank())
            {
                return act.getDomFeatureVector().getText().trim();
            }
            if (act.getDescription() != null && !act.getDescription().isBlank())
            {
                final Matcher m = QUOTED_PATTERN.matcher(act.getDescription());
                if (m.find())
                {
                    return m.group(1).trim();
                }
            }
        }

        if (stepVector != null && stepVector.getText() != null && !stepVector.getText().isBlank())
        {
            return stepVector.getText().trim();
        }

        return null;
    }

    private static String extractQuotedText(final String instruction)
    {
        if (instruction == null || instruction.isBlank())
        {
            return null;
        }
        final Matcher m = QUOTED_PATTERN.matcher(instruction);
        if (m.find())
        {
            return m.group(1).trim();
        }
        return null;
    }

    private static String generateDeterministicSplit(final List<Action> mutatingActions)
    {
        if (mutatingActions == null || mutatingActions.isEmpty())
        {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mutatingActions.size(); i++)
        {
            final Action a = mutatingActions.get(i);
            if (i > 0)
            {
                sb.append("\n");
            }
            if (a.getDescription() != null && !a.getDescription().isBlank())
            {
                sb.append(a.getDescription());
            }
            else
            {
                sb.append(a.getType()).append(" ").append(a.getTarget());
            }
        }
        return sb.toString();
    }
}
