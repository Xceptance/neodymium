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
package org.neodymium.ai.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.TargetExecutor;
import java.io.IOException;
import org.neodymium.ai.model.ExecutionMetrics;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookRecording;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.playbook.InlinePlaybookParser;
import org.neodymium.ai.prompt.ActionExtractionPrompt;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.util.Neodymium;

/**
 * Abstract class representing an execution session context that manages the active
 * execution context, LLM capabilities registry, event bus, SUT target executor,
 * and lifecycle hooks.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public abstract class AiSession implements AutoCloseable
{
    /**
     * The thread-isolated execution context.
     */
    private final ExecutionContext executionContext;

    /**
     * The LLM providers registry.
     */
    private final LlmRegistry llmRegistry;

    /**
     * The execution event bus.
     */
    private final ExecutionEventBus eventBus;

    /**
     * The target executor interacting with the SUT.
     */
    private final TargetExecutor targetExecutor;

    /**
     * The execution mode governing this session.
     */
    private final ExecutionMode executionMode;

    /**
     * Sequential pre-execution boundary hooks.
     */
    private final List<PreExecutionHook> preHooks = new CopyOnWriteArrayList<>();

    /**
     * Sequential post-execution boundary hooks.
     */
    private final List<PostExecutionHook> postHooks = new CopyOnWriteArrayList<>();

    /**
     * Constructs an AiSession with default LLM_ONLY execution mode.
     *
     * @param sessionData the session variables mapping
     * @param llmRegistry the provider registry
     * @param eventBus the event bus
     * @param targetExecutor the SUT target driver
     */
    protected AiSession(
        final SessionData sessionData,
        final LlmRegistry llmRegistry,
        final ExecutionEventBus eventBus,
        final TargetExecutor targetExecutor
    )
    {
        this(sessionData, llmRegistry, eventBus, targetExecutor, ExecutionMode.LLM_ONLY);
    }

    /**
     * Constructs an AiSession with explicit execution mode.
     *
     * @param sessionData the session variables mapping
     * @param llmRegistry the provider registry
     * @param eventBus the event bus
     * @param targetExecutor the SUT target driver
     * @param executionMode the execution mode
     */
    protected AiSession(
        final SessionData sessionData,
        final LlmRegistry llmRegistry,
        final ExecutionEventBus eventBus,
        final TargetExecutor targetExecutor,
        final ExecutionMode executionMode
    )
    {
        this.executionContext = new ExecutionContext(sessionData);
        this.llmRegistry = llmRegistry;
        this.eventBus = eventBus;
        this.targetExecutor = targetExecutor;
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.LLM_ONLY;
        this.eventBus.registerListener(new org.neodymium.ai.telemetry.TokenBudgetGuard());
    }

    /**
     * Retrieves the execution mode governing this session.
     *
     * @return the execution mode
     */
    public final ExecutionMode getExecutionMode()
    {
        return this.executionMode;
    }

    /**
     * Checks if this session executes in live LLM action generation mode.
     *
     * @return true if live execution mode, false otherwise
     */
    public final boolean isLive()
    {
        return this.executionMode.isLive();
    }

    /**
     * Checks if this session executes pre-recorded actions from replay cache.
     *
     * @return true if replay mode, false otherwise
     */
    public final boolean isReplay()
    {
        return this.executionMode.isReplay();
    }

    /**
     * Checks if this session automatically records executed actions.
     *
     * @return true if recording mode, false otherwise
     */
    public final boolean isRecording()
    {
        return this.executionMode.isRecording();
    }

    /**
     * Checks if this session supports self-healing on step replay failure.
     *
     * @return true if healing supported, false otherwise
     */
    public final boolean supportsHealing()
    {
        return this.executionMode.supportsHealing();
    }

    /**
     * Checks if this session executes under strict replay mode without LLM fallbacks.
     *
     * @return true if strict replay mode, false otherwise
     */
    public final boolean isStrictReplay()
    {
        return this.executionMode == ExecutionMode.REPLAY_STRICT;
    }

    /**
     * Retrieves the thread-isolated execution context.
     *
     * @return the execution context
     */
    public final ExecutionContext getExecutionContext()
    {
        return this.executionContext;
    }

    /**
     * Retrieves the session dataset container.
     *
     * @return the session data container
     */
    public final SessionData data()
    {
        return this.executionContext.getSessionData();
    }

    /**
     * Alias for {@link #data()} returning the session dataset container.
     *
     * @return the session data container
     */
    public final SessionData getData()
    {
        return data();
    }

    /**
     * Alias for {@link #data()} returning the session dataset container.
     *
     * @return the session data container
     */
    public final SessionData getSessionData()
    {
        return data();
    }

    /**
     * Retrieves a variable value from the session dataset container.
     *
     * @param key the variable key name
     * @return the variable object value, or null
     */
    public final Object getData(final String key)
    {
        return this.executionContext.getSessionData().get(key);
    }

    /**
     * Sets a non-sensitive variable in the dynamic dataset container.
     *
     * @param key the variable key name
     * @param value the variable object value
     */
    public final void setData(final String key, final Object value)
    {
        this.executionContext.getSessionData().set(key, value);
    }

    /**
     * Retrieves total LLM API calls made during this session.
     *
     * @return total LLM call count
     */
    public final int getTotalLlmCalls()
    {
        final Integer calls = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_TOTAL_LLM_CALLS);
        return calls != null ? calls : 0;
    }

    /**
     * Retrieves total replay steps executed from cache during this session.
     *
     * @return total replayed step count
     */
    public final int getTotalReplays()
    {
        final Integer replays = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_TOTAL_REPLAYS);
        return replays != null ? replays : 0;
    }

    /**
     * Retrieves total internal LLM prompt cache hits during this session.
     *
     * @return total internal cache hits
     */
    public final int getInternalCacheHits()
    {
        final Integer hits = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_INTERNAL_CACHE_HITS);
        return hits != null ? hits : 0;
    }

    /**
     * Computes an ExecutionMetrics snapshot instance for this session's active execution context state.
     *
     * @return execution metrics snapshot
     */
    public final ExecutionMetrics getMetrics()
    {
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> sessionSteps = (List<PlaybookStep>) this.executionContext.getTransientData().get("playbook.steps");
        final int stepCount = sessionSteps != null ? sessionSteps.size() : 0;
        final int healedCount = sessionSteps != null ? (int) sessionSteps.stream().filter(s -> s.getStatus() == PlaybookStepStatus.HEALED).count() : 0;
        final int softFailedCount = sessionSteps != null ? (int) sessionSteps.stream().filter(s -> s.isFailed() || s.getStatus() == PlaybookStepStatus.FAILED).count() : 0;

        final Integer stdCalls = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_STANDARD_CALL_COUNT);
        final Integer verifCalls = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_VERIFICATION_CALL_COUNT);
        final Integer pesapCalls = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_PESAP_CALL_COUNT);
        final Integer judgeCalls = (Integer) this.executionContext.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT);

        @SuppressWarnings("unchecked")
        final List<StepStats> stepStatsList = (List<StepStats>) this.executionContext.getTransientData().get("execution.stepStatsList");
        final Map<String, Integer> contextLevelCounts = new HashMap<>();
        int totalEscalations = 0;
        if (stepStatsList != null)
        {
            for (final StepStats stats : stepStatsList)
            {
                totalEscalations += aggregateStepStats(stats, contextLevelCounts);
            }
        }

        return new ExecutionMetrics(
            this.executionMode,
            getTotalLlmCalls(),
            stdCalls != null ? stdCalls : 0,
            verifCalls != null ? verifCalls : 0,
            pesapCalls != null ? pesapCalls : 0,
            judgeCalls != null ? judgeCalls : 0,
            stepCount,
            healedCount,
            softFailedCount,
            getTotalReplays(),
            getInternalCacheHits(),
            totalEscalations,
            contextLevelCounts,
            null
        );
    }

    /**
     * Retrieves the LLM provider registry.
     *
     * @return the LLM registry
     */
    public final LlmRegistry getLlmRegistry()
    {
        return this.llmRegistry;
    }

    /**
     * Retrieves the execution event bus.
     *
     * @return the event bus
     */
    public final ExecutionEventBus getEventBus()
    {
        return this.eventBus;
    }

    /**
     * Retrieves the target SUT executor.
     *
     * @return the target executor
     */
    public final TargetExecutor getTargetExecutor()
    {
        return this.targetExecutor;
    }

    /**
     * Registers a pre-execution hook.
     *
     * @param hook the pre-execution hook
     */
    public final void registerPreHook(final PreExecutionHook hook)
    {
        if (hook != null && !this.preHooks.contains(hook))
        {
            this.preHooks.add(hook);
        }
    }

    /**
     * Registers a post-execution hook.
     *
     * @param hook the post-execution hook
     */
    public final void registerPostHook(final PostExecutionHook hook)
    {
        if (hook != null && !this.postHooks.contains(hook))
        {
            this.postHooks.add(hook);
        }
    }

    /**
     * Executes all registered pre-execution hooks in registration order.
     */
    public final void runPreHooks()
    {
        for (final PreExecutionHook hook : this.preHooks)
        {
            hook.beforeSession(this);
        }
    }

    /**
     * Executes all registered post-execution hooks in registration order.
     *
     * @param success the final status outcome of the execution session
     */
    public final void runPostHooks(final boolean success)
    {
        for (final PostExecutionHook hook : this.postHooks)
        {
            hook.afterSession(this, success);
        }
    }

    /**
     * Programmatically executes a playbook in this session using the active target executor.
     *
     * @param playbook the playbook containing steps to run
     * @return the resulting playbook recording
     * @throws PipelineException if execution fails
     */
    public final PlaybookRecording execute(final Playbook playbook) throws PipelineException
    {
        return execute(playbook, null);
    }

    /**
     * Programmatically executes a playbook in this session with seeded dataset variables.
     *
     * @param playbook the playbook containing steps to run
     * @param sessionData parameter dataset values to seed into the execution context
     * @return the resulting playbook recording
     * @throws PipelineException if execution fails
     */
    public final PlaybookRecording execute(final Playbook playbook, final SessionData sessionData) throws PipelineException
    {
        if (playbook == null)
        {
            throw new IllegalArgumentException("Playbook must not be null.");
        }

        if (sessionData != null)
        {
            sessionData.getAllRawDataMap().forEach((k, v) -> this.executionContext.getSessionData().set(k, v));
        }

        this.executionContext.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, this.executionMode);
        this.executionContext.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);

        if (!this.executionContext.getTransientData().containsKey(ExecutionContext.KEY_ACTIVE_PROMPT))
        {
            this.executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new ActionExtractionPrompt());
        }
        this.executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_MODEL, Neodymium.aiConfiguration().aiModel());

        final List<PlaybookStep> playbookSteps = playbook.getSteps();

        @SuppressWarnings("unchecked")
        final List<PlaybookStep> sessionSteps = (List<PlaybookStep>) this.executionContext.getTransientData().get("playbook.steps");

        if (this.executionMode != null && this.executionMode.isReplay() && sessionSteps != null && !sessionSteps.isEmpty())
        {
            for (int i = 0; i < playbookSteps.size() && i < sessionSteps.size(); i++)
            {
                final PlaybookStep parsed = playbookSteps.get(i);
                final PlaybookStep recorded = sessionSteps.get(i);
                if (recorded.getActions() != null)
                {
                    parsed.setActions(recorded.getActions());
                }
            }
        }

        if (sessionSteps != null && sessionSteps != playbookSteps)
        {
            sessionSteps.clear();
            sessionSteps.addAll(playbookSteps);
        }

        for (int i = playbookSteps.size() - 1; i >= 0; i--)
        {
            this.executionContext.pushStep(
                ExecuteActionsStep.mapPlaybookStepToPipelineStep(playbookSteps.get(i), this, this.executionContext)
            );
        }

        try
        {
            final StateMachineRunner runner = new StateMachineRunner(this);
            runner.run();
        }
        catch (final Exception e)
        {
            Throwable root = e;
            while (root != null)
            {
                if (root instanceof AssertionError ae)
                {
                    throw ae;
                }
                if (root.getCause() == root)
                {
                    break;
                }
                root = root.getCause();
            }
            throw e;
        }

        final Map<String, Object> recordingMetadata = new HashMap<>(this.executionContext.getRecordingMetadata());
        recordingMetadata.put("totalLlmCalls", getTotalLlmCalls());
        recordingMetadata.put("standardCallCount", this.executionContext.getTransientData().getOrDefault(ExecutionContext.KEY_STANDARD_CALL_COUNT, 0));
        recordingMetadata.put("verificationCallCount", this.executionContext.getTransientData().getOrDefault(ExecutionContext.KEY_VERIFICATION_CALL_COUNT, 0));
        recordingMetadata.put("pesapCallCount", this.executionContext.getTransientData().getOrDefault(ExecutionContext.KEY_PESAP_CALL_COUNT, 0));
        recordingMetadata.put("judgeCallCount", this.executionContext.getTransientData().getOrDefault(ExecutionContext.KEY_JUDGE_CALL_COUNT, 0));
        recordingMetadata.put("totalReplays", getTotalReplays());
        recordingMetadata.put("internalCacheHits", getInternalCacheHits());

        @SuppressWarnings("unchecked")
        final List<StepStats> stepStatsList = (List<StepStats>) this.executionContext.getTransientData().get("execution.stepStatsList");
        final Map<String, Integer> contextLevelCounts = new HashMap<>();
        int totalEscalations = 0;
        if (stepStatsList != null)
        {
            for (final StepStats stats : stepStatsList)
            {
                totalEscalations += aggregateStepStats(stats, contextLevelCounts);
            }
        }
        recordingMetadata.put("totalEscalations", totalEscalations);
        recordingMetadata.put("contextLevelCounts", contextLevelCounts);

        return new PlaybookRecording(playbookSteps, recordingMetadata, this.executionMode);
    }

    private static int aggregateStepStats(final StepStats stats, final Map<String, Integer> contextLevelCounts)
    {
        if (stats == null)
        {
            return 0;
        }
        int escalations = 0;
        final List<String> levels = stats.getContextLevels();
        if (levels != null && !levels.isEmpty())
        {
            escalations += Math.max(0, levels.size() - 1);
            for (final String lvl : levels)
            {
                if (lvl != null && !lvl.isBlank())
                {
                    final String normalized = lvl.trim().toUpperCase();
                    contextLevelCounts.put(normalized, contextLevelCounts.getOrDefault(normalized, 0) + 1);
                }
            }
        }
        for (final StepStats child : stats.getSubStats())
        {
            escalations += aggregateStepStats(child, contextLevelCounts);
        }
        return escalations;
    }

    /**
     * Programmatically executes a multiline steps string in this session using the active target executor.
     *
     * @param stepsContent the raw multi-line steps content or YAML string
     * @return the resulting playbook recording
     * @throws PipelineException if parsing or execution fails
     */
    public final PlaybookRecording execute(final String stepsContent) throws PipelineException
    {
        return execute(stepsContent, null);
    }

    /**
     * Programmatically executes a multiline steps string in this session with seeded dataset variables.
     *
     * @param stepsContent the raw multi-line steps content or YAML string
     * @param sessionData parameter dataset values to seed into the execution context
     * @return the resulting playbook recording
     * @throws PipelineException if parsing or execution fails
     */
    public final PlaybookRecording execute(final String stepsContent, final SessionData sessionData) throws PipelineException
    {
        if (stepsContent == null)
        {
            throw new IllegalArgumentException("Steps content must not be null.");
        }

        try
        {
            final Playbook playbook = new InlinePlaybookParser(stepsContent).parse("inline", null);
            return execute(playbook, sessionData);
        }
        catch (final IOException e)
        {
            throw new ConclusiveFailureException("Failed to parse inline playbook content", e);
        }
    }

    /**
     * Closes the session and associated resources.
     *
     * @throws Exception if closing resources fails
     */
    @Override
    public abstract void close() throws Exception;

    /**
     * Static factory creating a Selenide browser automation session with default LLM_ONLY execution mode.
     *
     * @return a new Selenide browser session
     */
    public static AiSession selenide()
    {
        return selenide(ExecutionMode.LLM_ONLY);
    }

    /**
     * Static factory creating a Selenide browser automation session with custom initial session data and default LLM_ONLY mode.
     *
     * @param sessionData initial session parameters
     * @return a new Selenide browser session
     */
    public static AiSession selenide(final SessionData sessionData)
    {
        return selenide(ExecutionMode.LLM_ONLY, sessionData);
    }

    /**
     * Static factory creating a Selenide browser automation session.
     *
     * @param mode the execution mode
     * @return a new Selenide browser session
     */
    public static AiSession selenide(final ExecutionMode mode)
    {
        return new SelenideBrowserSession(mode);
    }

    /**
     * Static factory creating a Selenide browser session with initial session variables.
     *
     * @param mode the execution mode
     * @param sessionData initial session parameters
     * @return a new Selenide browser session
     */
    public static AiSession selenide(final ExecutionMode mode, final SessionData sessionData)
    {
        return new SelenideBrowserSession(mode, sessionData);
    }

    /**
     * Static factory creating a REST API automation session with default LLM_ONLY execution mode.
     *
     * @return a new REST API session
     */
    public static AiSession rest()
    {
        return rest(ExecutionMode.LLM_ONLY);
    }

    /**
     * Static factory creating a REST API automation session with custom initial session data and default LLM_ONLY mode.
     *
     * @param sessionData initial session parameters
     * @return a new REST API session
     */
    public static AiSession rest(final SessionData sessionData)
    {
        return rest(ExecutionMode.LLM_ONLY, sessionData);
    }

    /**
     * Static factory creating a REST API automation session.
     *
     * @param mode the execution mode
     * @return a new REST API session
     */
    public static AiSession rest(final ExecutionMode mode)
    {
        return new RestApiSession(mode);
    }

    /**
     * Static factory creating a REST API session with initial session variables.
     *
     * @param mode the execution mode
     * @param sessionData initial session parameters
     * @return a new REST API session
     */
    public static AiSession rest(final ExecutionMode mode, final SessionData sessionData)
    {
        return new RestApiSession(mode, sessionData);
    }

    /**
     * Static factory creating a Mock testing session with default LLM_ONLY execution mode.
     *
     * @return a new mock testing session
     */
    public static AiSession mock()
    {
        return mock(ExecutionMode.LLM_ONLY);
    }

    /**
     * Static factory creating a Mock testing session with custom initial session data and default LLM_ONLY mode.
     *
     * @param sessionData initial session parameters
     * @return a new mock testing session
     */
    public static AiSession mock(final SessionData sessionData)
    {
        return mock(ExecutionMode.LLM_ONLY, sessionData);
    }

    /**
     * Static factory creating a Mock testing session with default parameters.
     *
     * @param mode the execution mode
     * @return a new mock testing session
     */
    public static AiSession mock(final ExecutionMode mode)
    {
        return mock(mode, new SessionData());
    }

    /**
     * Static factory creating a Mock testing session with initial session data.
     *
     * @param mode the execution mode
     * @param sessionData initial session parameters
     * @return a new mock testing session
     */
    public static AiSession mock(final ExecutionMode mode, final SessionData sessionData)
    {
        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(createMockLlmProvider());
        return new MockSession(sessionData, registry, new ExecutionEventBus(), new MockTargetExecutor(), mode);
    }

    /**
     * Static shortcut method to execute a playbook using a default Selenide browser session in the specified execution mode.
     *
     * @param mode the execution mode governing the session
     * @param playbook the playbook containing steps to run
     * @return the resulting playbook recording
     * @throws Exception if execution or resource closing fails
     */
    public static PlaybookRecording execute(final ExecutionMode mode, final Playbook playbook) throws Exception
    {
        return execute(mode, playbook, null);
    }

    /**
     * Static shortcut method to execute a playbook with session data using a default Selenide browser session in the specified execution mode.
     *
     * @param mode the execution mode governing the session
     * @param playbook the playbook containing steps to run
     * @param sessionData parameter dataset values to seed into the execution context
     * @return the resulting playbook recording
     * @throws Exception if execution or resource closing fails
     */
    public static PlaybookRecording execute(final ExecutionMode mode, final Playbook playbook, final SessionData sessionData) throws Exception
    {
        try (final AiSession session = selenide(mode, sessionData))
        {
            return session.execute(playbook, sessionData);
        }
    }

    private static LlmProvider createMockLlmProvider()
    {
        return new LlmProvider()
        {
            @Override
            public LlmResponse chat(final LlmRequest request)
            {
                return new LlmResponse("[]", new TokenUsage(10, 0, 10), "mock-model");
            }

            @Override
            public Set<LlmCapability> getCapabilities()
            {
                return Set.of(LlmCapability.values());
            }
        };
    }

    /**
     * Static factory method to instantiate a Mock session with custom mocks for unit testing.
     *
     * @param data the session variables
     * @param registry the LLM registry
     * @param bus the event bus
     * @param executor the target driver
     * @return a mock execution session
     */
    public static AiSession mock(
        final ExecutionMode mode,
        final SessionData data,
        final LlmRegistry registry,
        final ExecutionEventBus bus,
        final TargetExecutor executor
    )
    {
        return new MockSession(data, registry, bus, executor, mode != null ? mode : ExecutionMode.LLM_ONLY);
    }

    public static AiSession mock(
        final SessionData data,
        final LlmRegistry registry,
        final ExecutionEventBus bus,
        final TargetExecutor executor
    )
    {
        return mock(ExecutionMode.LLM_ONLY, data, registry, bus, executor);
    }
}
