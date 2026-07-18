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
package org.neodymium.ai.pipeline.steps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.prompt.ActionSanitizer;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.prompt.PesapPrompt;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.session.AiSession;

/**
 * Concrete pipeline step executing actions parsed from LLM responses, sanitizing/parameterizing
 * their inputs on the fly, storing execution history, and dispatching executed events.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStep implements PipelineStep
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class);

    /**
     * The sanitizer used for variable parameterization of recorded actions.
     */
    private final ActionSanitizer actionSanitizer = new DefaultActionSanitizer();

    /**
     * Constructs an ExecuteActionsStep.
     */
    public ExecuteActionsStep()
    {
    }

    /**
     * Retrieves the LLM actions result, executes each action, sanitizes them,
     * appends them to the session recording log, and dispatches status events.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if an action fails (triggers HealingRequiredException)
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        // Retrieve the active session from context transient storage
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        // Retrieve SUT target executor driving browser/REST operations
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        if (session == null)
        {
            throw new ConclusiveFailureException("No active AiSession registered in ExecutionContext transient data");
        }
        if (executor == null)
        {
            throw new ConclusiveFailureException("No active TargetExecutor registered in ExecutionContext transient data");
        }

        // Retrieve the list of actions returned by the prior CallLlmStep execution
        @SuppressWarnings("unchecked")
        final List<Action> actions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_LAST_LLM_RESULT);
        if (actions == null)
        {
            return;
        }

        // Initialize or fetch the concurrent recording collection tracking all executed playbooks actions
        @SuppressWarnings("unchecked")
        final List<Action> recordedActions = (List<Action>) context.getTransientData()
            .computeIfAbsent(ExecutionContext.KEY_RECORDING, k -> new CopyOnWriteArrayList<>());

        // Execute each parsed action sequentially
        for (final Action action : actions)
        {
            if (action != null)
            {
                executeSingleAction(action, executor, recordedActions, session, context);
            }
        }
    }

    /**
     * Helper executing a single action, applying sanitization, logging, and throwing healing exceptions on failure.
     */
    private void executeSingleAction(
        final Action action,
        final TargetExecutor executor,
        final List<Action> recordedActions,
        final AiSession session,
        final ExecutionContext context
    ) throws PipelineException
    {
        ExecutionContext.setActiveContext(context);
        try
        {
            if (action == null)
            {
                return;
            }

            // Intercept control / assertion actions that require no SUT execution
            if ("NONE".equalsIgnoreCase(action.getType()) || "VERIFY".equalsIgnoreCase(action.getType()))
            {
                return;
            }

            // Intercept INCLUDE control actions to perform dynamic runtime inclusion expansion
            if (action.getType().equalsIgnoreCase("INCLUDE"))
            {
                executeIncludeAction(action, session, context, recordedActions);
                return;
            }

            final Action resolvedAction = resolveActionVariables(action, context.getSessionData());

            try
            {
                LOGGER.debug("   ▶️ [Action] Type:        {}", resolvedAction.getType());
                if (resolvedAction.getDescription() != null && !resolvedAction.getDescription().trim().isEmpty())
                {
                    LOGGER.debug("      🤖 Description: {}", resolvedAction.getDescription());
                }
                if (resolvedAction.getTarget() != null && !resolvedAction.getTarget().trim().isEmpty())
                {
                    LOGGER.debug("      🎯 Target:      {}", resolvedAction.getTarget());
                }
                final String val = resolvedAction.getValue();
                if (val != null && !val.trim().isEmpty())
                {
                    LOGGER.debug("      💵 Value:       {}", val);
                }

                // Execute SUT action via targeted SUT driver
                try
                {
                    context.getTransientData().put("currentAction", resolvedAction);
                    executor.execute(resolvedAction);
                }
                finally
                {
                    context.getTransientData().remove("currentAction");
                }
                
                // Mask any raw sensitive inputs dynamically matching SessionData variable keys
                final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
                final PlaybookStep step = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                if (step != null)
                {
                     sanitized.setStepInstruction(step.getInstruction());
                     sanitized.setStepLine(step.getLineNumber());
                     sanitized.setStepFile(step.getSourceFile());
                      final org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
                      final boolean isNoReplay = step.isNoReplay();
                      if (mode != null && (!mode.isReplay() || isNoReplay))
                      {
                          if (isNoReplay && Boolean.TRUE.equals(context.getTransientData().get("KEY_CURRENT_STEP_FIRST_ACTION")))
                          {
                              step.getActions().clear();
                              context.getTransientData().put("KEY_CURRENT_STEP_FIRST_ACTION", false);
                          }
                          step.getActions().add(sanitized);
                      }
                }
                
                // Log to local recording and dispatch verification updates to active event listeners
                recordedActions.add(sanitized);
                @SuppressWarnings("unchecked")
                final List<Action> stepActions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
                if (stepActions != null)
                {
                    stepActions.add(sanitized);
                }
                session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
            }
            catch (final IOException e)
            {
                // Dispatch failed event status and throw HealingRequiredException to initiate recovery
                session.getEventBus().dispatch(new ActionExecutedEvent(action, false));
                throw new HealingRequiredException("Action execution failed against SUT: " + action.getDescription(), e);
            }
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    /**
     * Executes INCLUDE control action dynamically by resolving, parsing, and pushing steps on stack.
     */
    private void executeIncludeAction(
        final Action action,
        final AiSession session,
        final ExecutionContext context,
        final List<Action> recordedActions
    ) throws PipelineException
    {
        // Extract include path target from SUT action definition
        final String pathTemp = action.getTarget();
        final String path = (pathTemp == null || pathTemp.trim().isEmpty()) ? action.getValue() : pathTemp;
        if (path == null || path.trim().isEmpty())
        {
            throw new ConclusiveFailureException("INCLUDE action target path is null or empty");
        }

        // Retrieve registered PlaybookResourceManager from the context state
        final PlaybookResourceManager manager = (PlaybookResourceManager) context.getTransientData().get(ExecutionContext.KEY_RESOURCE_MANAGER);
        if (manager == null)
        {
            throw new ConclusiveFailureException("No PlaybookResourceManager registered in ExecutionContext transient data");
        }

        // Retrieve or instantiate the default YamlPlaybookParser
        PlaybookParser parser = (PlaybookParser) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_PARSER);
        if (parser == null)
        {
            parser = new YamlPlaybookParser();
        }

        // Fetch thread-safe stack listing active includes to detect cycle inclusions
        @SuppressWarnings("unchecked")
        final List<String> runtimeStack = (List<String>) context.getTransientData()
            .computeIfAbsent(ExecutionContext.KEY_RUNTIME_INCLUDE_STACK, k -> new ArrayList<>());

        if (runtimeStack.contains(path))
        {
            throw new ConclusiveFailureException("Circular dynamic inclusion detected: " + String.join(" -> ", runtimeStack) + " -> " + path);
        }

        // Resolve absolute or relative path context based on active parent directory
        final String currentParent = (String) context.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_PLAYBOOK_IDENTIFIER, "");
        final String resolvedIdentifier = manager.resolveInclude(currentParent, path);

        // Add to callstack before parsing to cover circular validations
        runtimeStack.add(path);
        try
        {
            // Parse included YAML playbook target
            final Playbook playbook = parser.parse(resolvedIdentifier, manager);
            final List<PlaybookStep> playbookSteps = playbook.getSteps();

            final PlaybookStep includeStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
            // Push included sub-steps onto LIFO stack in reverse order to ensure sequential execution
            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                final PlaybookStep step = playbookSteps.get(i);
                if (includeStep != null)
                {
                    step.setParent(includeStep);
                }
                final PipelineStep stepPipeline = mapPlaybookStepToPipelineStep(step, session, context);
                context.pushStep(stepPipeline);
            }
            
            // Parameterize and log the INCLUDE step record in history
            final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
            recordedActions.add(sanitized);
            @SuppressWarnings("unchecked")
            final List<Action> stepActions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
            if (stepActions != null)
            {
                stepActions.add(sanitized);
            }
            session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
        }
        catch (final IOException e)
        {
            throw new ConclusiveFailureException("Failed to read or parse included playbook: " + path, e);
        }
        finally
        {
            // Clean stack isolation frame
            runtimeStack.remove(runtimeStack.size() - 1);
        }
    }

    /**
     * Maps parsed PlaybookStep to concrete PipelineStep execution tree.
     */
    public static PipelineStep mapPlaybookStepToPipelineStep(
        final PlaybookStep step,
        final AiSession session,
        final ExecutionContext context
    )
    {
        // For composite steps, map and execute all children sequentially using SequenceStep
        if (step.isComposite())
        {
            final List<PipelineStep> subPipelineSteps = new ArrayList<>();
            for (final PlaybookStep subStep : step.getSubSteps())
            {
                subPipelineSteps.add(mapPlaybookStepToPipelineStep(subStep, session, context));
            }
            return new SequenceStep(subPipelineSteps);
        }

        // For leaf steps, return a pipeline step wrapper setting the active instruction and pushing execution loop
        return contextState -> {
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);
            final String rawInstruction = step.getInstruction();
            final String resolvedInstruction = contextState.getSessionData().resolveVariables(rawInstruction);
            final String preparedInstruction = prepareInstruction(resolvedInstruction);
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, preparedInstruction);
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_STEP_ACTIONS, new CopyOnWriteArrayList<Action>());

            final boolean stepNoReplay = step.isNoReplay();
            contextState.getTransientData().put("KEY_CURRENT_STEP_NO_REPLAY", stepNoReplay);
            if (stepNoReplay)
            {
                contextState.getTransientData().put("KEY_CURRENT_STEP_FIRST_ACTION", true);
            }

            @SuppressWarnings("unchecked")
            List<org.neodymium.ai.pipeline.StepStats> allStats = (List<org.neodymium.ai.pipeline.StepStats>) contextState.getTransientData().get("execution.stepStatsList");
            if (allStats == null)
            {
                allStats = new java.util.ArrayList<>();
                contextState.getTransientData().put("execution.stepStatsList", allStats);
            }

            @SuppressWarnings("unchecked")
            final Map<PlaybookStep, org.neodymium.ai.pipeline.StepStats> stepStatsMap =
                (Map<PlaybookStep, org.neodymium.ai.pipeline.StepStats>) contextState.getTransientData()
                    .computeIfAbsent("execution.stepStatsMap", k -> new java.util.HashMap<>());

            final org.neodymium.ai.config.ExecutionMode executionMode = (org.neodymium.ai.config.ExecutionMode) contextState.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
            final boolean isReplayStats = executionMode != null && executionMode.isReplay() && !stepNoReplay;

            final org.neodymium.ai.pipeline.StepStats stats = getOrCreateStatsForStep(step, System.currentTimeMillis(), isReplayStats, stepStatsMap, allStats);
            contextState.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

            @SuppressWarnings("unchecked")
            final Set<PlaybookStep> alreadySplitSteps = (Set<PlaybookStep>) contextState.getTransientData()
                .computeIfAbsent("pesap.alreadySplitSteps", k -> new HashSet<>());

            org.neodymium.ai.executor.selenide.ContextLevel initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.LEAN;
            final String lower = resolvedInstruction.toLowerCase();
            if (lower.contains("(visual)"))
            {
                initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_LEAN;
            }
            else if (lower.contains("(layout)"))
            {
                initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL;
            }
            else if (lower.contains("(hint:"))
            {
                initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.HINT;
            }

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> flatSteps = (List<PlaybookStep>) contextState.getTransientData().get("playbook.flatSteps");

            final boolean isReplayMode = executionMode != null && executionMode.isReplay() && !stepNoReplay;
            final org.neodymium.ai.config.AiConfiguration config = new org.neodymium.ai.config.AiConfiguration();
            if (!isReplayMode && config.getBoolean("neodymium.ai.pesap.enabled", true) && !alreadySplitSteps.contains(step))
            {
                alreadySplitSteps.add(step);
                try
                {
                    String previousInstruction = null;
                    final List<String> nextInstructions = new ArrayList<>();
                    if (flatSteps != null)
                    {
                        final int idx = flatSteps.indexOf(step);
                        if (idx != -1)
                        {
                            if (idx > 0)
                            {
                                previousInstruction = contextState.getSessionData().resolveVariables(flatSteps.get(idx - 1).getInstruction());
                            }
                            for (int i = idx + 1; i < flatSteps.size() && nextInstructions.size() < 2; i++)
                            {
                                nextInstructions.add(contextState.getSessionData().resolveVariables(flatSteps.get(i).getInstruction()));
                            }
                        }
                    }

                    final PesapPrompt pesapPrompt = new PesapPrompt(resolvedInstruction, previousInstruction, nextInstructions);
                    final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.STEP_SPLITTING);
                    final double temp = config.getTemperature("action");
                    final int timeoutSeconds = config.getTimeoutSeconds("action");

                    final LlmRequest request = new LlmRequest(
                        pesapPrompt.compileSystemMessage(contextState),
                        pesapPrompt.compileUserMessage(contextState),
                        Collections.emptyList(),
                        pesapPrompt.getResponseSchema(),
                        temp,
                        timeoutSeconds
                    );

                    LOGGER.debug("================================================================================");
                    LOGGER.debug("💬 [Pre-Step PESAP] Running analysis for: \"{}\" using provider '{}'", resolvedInstruction, provider.getClass().getSimpleName());
                    LOGGER.debug("================================================================================");
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace("System Prompt:\n{}", request.systemMessage());
                        LOGGER.trace("User Prompt:\n{}", request.userMessage());
                    }

                    final long startTime = System.currentTimeMillis();
                    final LlmResponse response = provider.chat(request);
                    final long durationMs = System.currentTimeMillis() - startTime;

                    LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)", response.content() != null ? response.content().length() : 0, durationMs);
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace("Raw response content:\n{}", CallLlmStep.formatJsonForLogging(response.content()));
                    }

                    final org.neodymium.ai.client.TokenUsage newUsage = response.tokenUsage();
                    if (newUsage != null)
                    {
                        stats.addPesapCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());

                        LOGGER.debug("   📊 [Pre-Step PESAP] Tokens: {} in ({} cached) → {} out (total: {})",
                            newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                        final org.neodymium.ai.client.TokenUsage existing = (org.neodymium.ai.client.TokenUsage) contextState.getTransientData().get(ExecutionContext.KEY_PESAP_TOKEN_USAGE);
                        if (existing == null)
                        {
                            contextState.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, newUsage);
                        }
                        else
                        {
                            contextState.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, new org.neodymium.ai.client.TokenUsage(
                                existing.inputTokenCount() + newUsage.inputTokenCount(),
                                existing.outputTokenCount() + newUsage.outputTokenCount(),
                                existing.totalTokenCount() + newUsage.totalTokenCount(),
                                existing.cachedTokenCount() + newUsage.cachedTokenCount()
                            ));
                        }
                    }

                    final PesapPrompt.PesapResult pesapResult = pesapPrompt.parseResponse(response.content(), contextState);

                    if (pesapResult.splitSteps() != null && pesapResult.splitSteps().size() > 1)
                    {
                        LOGGER.info("✂️ Upfront JIT step split detected: \"{}\" split into {}", resolvedInstruction, pesapResult.splitSteps());
                        for (final String part : pesapResult.splitSteps())
                        {
                            final PlaybookStep subStep = new PlaybookStep(part);
                            subStep.setSourceFile(step.getSourceFile());
                            subStep.setLineNumber(step.getLineNumber());
                            subStep.setParent(step);
                            step.getSubSteps().add(subStep);
                        }

                        final List<PipelineStep> subPipelineSteps = new ArrayList<>();
                        for (final PlaybookStep subStep : step.getSubSteps())
                        {
                            subPipelineSteps.add(mapPlaybookStepToPipelineStep(subStep, session, context));
                        }
                        for (int i = subPipelineSteps.size() - 1; i >= 0; i--)
                        {
                            contextState.pushStep(subPipelineSteps.get(i));
                        }
                        return;
                    }

                    if (pesapResult.contextLevel() != null)
                    {
                        try
                        {
                            initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.valueOf(pesapResult.contextLevel().toUpperCase().trim());
                        }
                        catch (final Exception e)
                        {
                            // Keep default
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("⚠️ Pre-Step PESAP failed for step '{}' — falling back to defaults: {}", resolvedInstruction, e.getMessage());
                }
            }

            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, initialLevel);
            stats.getContextLevels().add(initialLevel.name());
            String stepIndicator = "";
            if (flatSteps != null)
            {
                final int idx = flatSteps.indexOf(step);
                if (idx != -1)
                {
                    stepIndicator = " " + (idx + 1) + "/" + flatSteps.size();
                }
            }

            final String sourceFile = step.getSourceFile();
            final int lineNumber = step.getLineNumber();
            final String locationInfo;
            if (sourceFile != null && lineNumber != -1)
            {
                locationInfo = " " + sourceFile + ":" + lineNumber;
            }
            else if (sourceFile != null)
            {
                locationInfo = " " + sourceFile;
            }
            else
            {
                locationInfo = "";
            }

            LOGGER.debug("--------------------------------------------------------------------------------");
            LOGGER.debug("👉 [Executing Step{}]{}", stepIndicator, locationInfo);
            LOGGER.debug("       Instruction: \"{}\"", resolvedInstruction);
            LOGGER.debug("--------------------------------------------------------------------------------");

            @SuppressWarnings("unchecked")
            final AiPrompt<List<Action>> activePrompt = (AiPrompt<List<Action>>) contextState.getTransientData()
                .get(ExecutionContext.KEY_ACTIVE_PROMPT);

            if (activePrompt == null)
            {
                throw new ConclusiveFailureException("No active prompt template registered in ExecutionContext transient data");
            }

            final org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) contextState.getTransientData()
                .computeIfAbsent(ExecutionContext.KEY_EXECUTION_MODE, k -> new org.neodymium.ai.config.AiConfiguration().getExecutionMode());

            // Check if we are in replay mode and have a recorded dHash for this step
            if (mode.isReplay() && !stepNoReplay && step.isVisualStep() && step.getScreenshotHash() != null && !step.getScreenshotHash().isEmpty())
            {
                final TargetExecutor executor = (TargetExecutor) contextState.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                if (executor != null)
                {
                    try
                    {
                        final SutState currentState = executor.captureState(org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_LEAN);
                        contextState.getTransientData().put(ExecutionContext.KEY_LAST_STATE, currentState);

                        String currentHash = null;
                        if (currentState != null && currentState.getAttachments() != null)
                        {
                            for (final SutAttachment attachment : currentState.getAttachments())
                            {
                                if (attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
                                {
                                    currentHash = com.xceptance.neodymium.ai.util.ScreenshotHasher.computeHash(attachment.base64Data());
                                    break;
                                }
                            }
                        }

                        if (currentHash != null)
                        {
                            final int distance = com.xceptance.neodymium.ai.util.ScreenshotHasher.getHammingDistance(step.getScreenshotHash(), currentHash);
                            final boolean hasActualActions = step.getActions() != null && !step.getActions().isEmpty()
                                && step.getActions().stream().anyMatch(a -> !"NONE".equalsIgnoreCase(a.getType()));

                            if (distance <= 10 && !hasActualActions)
                            {
                                org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).info(
                                    "   ✅ Visual dHash match (distance: {} <= 10) for instruction: \"{}\". Bypassing LLM call/actions.",
                                    distance, resolvedInstruction);
                                // Visual states match perfectly! We can return directly and bypass LLM call and actions!
                                return;
                            }
                            else
                            {
                                if (hasActualActions)
                                {
                                    if (distance <= 10)
                                    {
                                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).info(
                                            "   Visual dHash match (distance: {} <= 10) for interactive instruction: \"{}\". Executing actions anyway to guarantee state.",
                                            distance, resolvedInstruction);
                                    }
                                    else
                                    {
                                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).debug(
                                            "   Visual dHash mismatch (distance: {} > 10) for interactive instruction: \"{}\". Proceeding to execute actions.",
                                            distance, resolvedInstruction);
                                    }
                                }
                                else
                                {
                                    final String msg = String.format("Visual dHash mismatch (distance: %d > 10) for instruction: \"%s\".", distance, resolvedInstruction);
                                    org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("   ❌ " + msg);
                                    if (mode.supportsHealing())
                                    {
                                        throw new HealingRequiredException(msg);
                                    }
                                    else
                                    {
                                        throw new DivergenceException(msg);
                                    }
                                }
                            }
                        }
                    }
                    catch (final PipelineException e)
                    {
                        throw e;
                    }
                    catch (final Exception e)
                    {
                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("   ⚠️ Failed to capture state or compare visual dHash: {}", e.getMessage());
                    }
                }
            }

            // Assemble try block sequence: [CaptureStateStep] -> [CallLlmStep | Replay Actions] -> ExecuteActionsStep -> VerifyOutcomeStep
            final ExecuteActionsStep executeStep = new ExecuteActionsStep();
            final VerifyOutcomeStep verifyStep = new VerifyOutcomeStep();

            final List<PipelineStep> standardFlow = new ArrayList<>();

            final boolean isReplay = mode.isReplay() && !stepNoReplay;

            if (isReplay)
            {
                // Replay mode: Put recorded actions directly into KEY_LAST_LLM_RESULT without querying LLM
                standardFlow.add(c -> {
                    c.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, step.getActions() != null ? step.getActions() : List.of());
                    final Integer replays = (Integer) c.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
                    c.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, replays + 1);
                });
            }
            else
            {
                // Live mode: Query LLM for actions
                final boolean verificationEnabled = new org.neodymium.ai.config.AiConfiguration().isSemanticVerificationEnabled();
                final org.neodymium.ai.executor.selenide.ContextLevel captureLevel;
                if (verificationEnabled && initialLevel == org.neodymium.ai.executor.selenide.ContextLevel.LEAN)
                {
                    captureLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_LEAN;
                }
                else
                {
                    captureLevel = initialLevel;
                }

                standardFlow.add(c -> {
                    final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    try
                    {
                        final SutState state = executor.captureState(captureLevel);
                        c.getTransientData().put(ExecutionContext.KEY_LAST_STATE, state);
                    }
                    catch (final java.io.IOException e)
                    {
                        throw new ConclusiveFailureException("Failed to capture SUT state before execution", e);
                    }
                });

                final LlmCapability capability = (initialLevel != null && initialLevel.includesScreenshot()) ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(activePrompt, capability);
                standardFlow.add(llmStep);
            }

            standardFlow.add(executeStep);
            standardFlow.add(verifyStep);

            final SequenceStep tryBlock = new SequenceStep(standardFlow);

            // Register exception handlers based on execution mode
            final Map<Class<? extends PipelineException>, PipelineStep> handlers = new HashMap<>();

            if (mode.isLive() || (!isReplay && mode.supportsHealing()))
            {
                // Live Escalation: PrepareRetryStep -> CallLlmStep -> ExecuteActionsStep -> VerifyOutcomeStep
                handlers.put(HealingRequiredException.class, c -> {
                    final PrepareRetryStep prepareStep = new PrepareRetryStep();
                    final CallLlmStep<List<Action>> escalationLlmStep = new CallLlmStep<>(activePrompt, LlmCapability.TEXT_ONLY);
                    
                    c.pushStep(verifyStep);
                    c.pushStep(executeStep);
                    c.pushStep(escalationLlmStep);
                    c.pushStep(prepareStep);
                });

                handlers.put(org.neodymium.ai.pipeline.ToLevelEscalationException.class, c -> {
                    final org.neodymium.ai.pipeline.ToLevelEscalationException e = (org.neodymium.ai.pipeline.ToLevelEscalationException) c.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);
                    final String targetLevelStr = e.getTargetLevel();
                    org.neodymium.ai.executor.selenide.ContextLevel targetLevel = org.neodymium.ai.executor.selenide.ContextLevel.LEAN;
                    try
                    {
                        targetLevel = org.neodymium.ai.executor.selenide.ContextLevel.valueOf(targetLevelStr.toUpperCase());
                        c.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, targetLevel);
                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("⚠️ Context escalated to: {}", targetLevel);

                        final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                        if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stepStats)
                        {
                            stepStats.getContextLevels().add(targetLevel.name());
                        }
                    }
                    catch (final Exception ex)
                    {
                        // Fallback or ignore invalid level
                    }
                    
                    final LlmCapability escalationCapability = (targetLevel != null && targetLevel.includesScreenshot()) ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                    final CallLlmStep<List<Action>> escalationLlmStep = new CallLlmStep<>(activePrompt, escalationCapability);
                    
                    c.pushStep(verifyStep);
                    c.pushStep(executeStep);
                    c.pushStep(escalationLlmStep);
                    c.pushStep(new CaptureStateStep());
                });
            }
            else if (mode.supportsHealing() && isReplay && !step.isOptional())
            {
                // Replay Healing: PrepareRetryStep -> SemanticDivergenceAnalysisStep -> CallLlmStep -> ExecuteActionsStep -> VerifyOutcomeStep
                handlers.put(HealingRequiredException.class, c -> {
                    final PrepareRetryStep prepareStep = new PrepareRetryStep();
                    final SemanticDivergenceAnalysisStep diffStep = new SemanticDivergenceAnalysisStep();
                    final CallLlmStep<List<Action>> healLlmStep = new CallLlmStep<>(activePrompt, LlmCapability.TEXT_ONLY);
                    
                    c.pushStep(verifyStep);
                    c.pushStep(executeStep);
                    c.pushStep(healLlmStep);
                    c.pushStep(diffStep);
                    c.pushStep(prepareStep);
                });
            }
            // If REPLAY_STRICT, no HealingRequiredException handler is registered; exception escapes to trigger Visual RCA

            final TryCatchStep tryCatch = new TryCatchStep(tryBlock, handlers);

            // Push end-hook step first, so it runs AFTER tryCatch executes
            contextState.pushStep(c -> {
                final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stepStats)
                {
                    stepStats.setDurationMs(System.currentTimeMillis() - stepStats.getStartTime());
                    @SuppressWarnings("unchecked")
                    final List<Action> stepActions = (List<Action>) c.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
                    if (stepActions != null)
                    {
                        stepStats.getActions().addAll(stepActions);
                    }
                }
            });

            contextState.pushStep(tryCatch);
        };
    }

    private static org.neodymium.ai.pipeline.StepStats getOrCreateStatsForStep(
        final PlaybookStep step,
        final long startTime,
        final boolean replayed,
        final Map<PlaybookStep, org.neodymium.ai.pipeline.StepStats> stepStatsMap,
        final List<org.neodymium.ai.pipeline.StepStats> allStats
    )
    {
        org.neodymium.ai.pipeline.StepStats stats = stepStatsMap.get(step);
        if (stats == null)
        {
            stats = new org.neodymium.ai.pipeline.StepStats(step.getInstruction(), startTime);
            stats.setReplayed(replayed);
            stepStatsMap.put(step, stats);

            final PlaybookStep parentStep = step.getParent();
            if (parentStep != null)
            {
                final org.neodymium.ai.pipeline.StepStats parentStats = getOrCreateStatsForStep(parentStep, startTime, replayed, stepStatsMap, allStats);
                parentStats.getSubStats().add(stats);
            }
            else
            {
                allStats.add(stats);
            }
        }
        return stats;
    }

    private Action resolveActionVariables(final Action rawAction, final org.neodymium.ai.model.SessionData data)
    {
        if (rawAction == null || data == null)
        {
            return rawAction;
        }

        // 1. Resolve values list
        final List<String> resolvedValues = new ArrayList<>();
        for (final String val : rawAction.getValues())
        {
            resolvedValues.add(val != null ? data.resolveVariables(val) : null);
        }

        // 2. Resolve target selector/URL
        final String resolvedTarget = rawAction.getTarget() != null ? data.resolveVariables(rawAction.getTarget()) : null;

        // 3. Resolve description
        final String resolvedDesc = rawAction.getDescription() != null ? data.resolveVariables(rawAction.getDescription()) : null;

        // Construct the new resolved action
        final Action resolvedAction = new Action(
            rawAction.getType(),
            resolvedTarget,
            resolvedValues,
            resolvedDesc,
            rawAction.getReasoning()
        );

        // Copy dynamic parameters map
        resolvedAction.getParameters().putAll(rawAction.getParameters());

        // Recursively resolve nested branch/conditional action lists
        if (rawAction.getCondition() != null)
        {
            final List<Action> resolvedCondition = new ArrayList<>();
            for (final Action condAct : rawAction.getCondition())
            {
                resolvedCondition.add(resolveActionVariables(condAct, data));
            }
            resolvedAction.setCondition(resolvedCondition);
        }
        
        if (rawAction.getThen() != null)
        {
            final List<Action> resolvedThen = new ArrayList<>();
            for (final Action thenAct : rawAction.getThen())
            {
                resolvedThen.add(resolveActionVariables(thenAct, data));
            }
            resolvedAction.setThen(resolvedThen);
        }
        
        if (rawAction.getElseActions() != null)
        {
            final List<Action> resolvedElse = new ArrayList<>();
            for (final Action elseAct : rawAction.getElseActions())
            {
                resolvedElse.add(resolveActionVariables(elseAct, data));
            }
            resolvedAction.setElseActions(resolvedElse);
        }

        return resolvedAction;
    }

    /**
     * Prepares the instruction by stripping all explicit control tags case-insensitively.
     *
     * @param instruction the raw instruction prompt
     * @return the cleaned instruction string
     */
    public static String prepareInstruction(final String instruction)
    {
        if (instruction == null)
        {
            return null;
        }
        String prepared = instruction;
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*no-replay\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*bug(?:\\s*:\\s*[^)]+)?\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*(optional|soft)\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*timeout\\s*:\\s*\\d+(?:ms|s)?\\)\\s*", " ");
        return prepared.replaceAll("\\s+", " ").trim();
    }
}
