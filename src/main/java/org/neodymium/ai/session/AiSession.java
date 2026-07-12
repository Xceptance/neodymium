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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;

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
     * Sequential pre-execution boundary hooks.
     */
    private final List<PreExecutionHook> preHooks = new CopyOnWriteArrayList<>();

    /**
     * Sequential post-execution boundary hooks.
     */
    private final List<PostExecutionHook> postHooks = new CopyOnWriteArrayList<>();

    /**
     * Constructs an AiSession.
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
        this.executionContext = new ExecutionContext(sessionData);
        this.llmRegistry = llmRegistry;
        this.eventBus = eventBus;
        this.targetExecutor = targetExecutor;
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
     * Closes the session and associated resources.
     *
     * @throws Exception if closing resources fails
     */
    @Override
    public abstract void close() throws Exception;

    /**
     * Static factory method to instantiate a Mock session for unit testing.
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
        return new MockSession(data, registry, bus, executor);
    }
}
