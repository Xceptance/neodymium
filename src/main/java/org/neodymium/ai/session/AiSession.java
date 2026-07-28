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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;
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
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookRecording;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
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
     * Retrieves the thread-isolated execution context.
     *
     * @return the execution context
     */
    public final ExecutionContext getExecutionContext()
    {
        return this.executionContext;
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
        for (int i = playbookSteps.size() - 1; i >= 0; i--)
        {
            this.executionContext.pushStep(
                ExecuteActionsStep.mapPlaybookStepToPipelineStep(playbookSteps.get(i), this, this.executionContext)
            );
        }

        final StateMachineRunner runner = new StateMachineRunner(this);
        runner.run();

        return new PlaybookRecording(playbookSteps);
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
        final SessionData data,
        final LlmRegistry registry,
        final ExecutionEventBus bus,
        final TargetExecutor executor
    )
    {
        return new MockSession(data, registry, bus, executor, ExecutionMode.LLM_ONLY);
    }
}
