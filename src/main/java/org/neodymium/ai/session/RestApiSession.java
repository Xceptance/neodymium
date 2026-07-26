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

import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.rest.RestTargetExecutor;
import org.neodymium.ai.model.SessionData;

/**
 * Package-private {@link AiSession} implementation tailored for REST API automation
 * backed by {@link RestTargetExecutor}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
class RestApiSession extends AiSession
{
    /**
     * Constructs a RestApiSession with default session data.
     *
     * @param executionMode the mode governing session execution
     */
    RestApiSession(final ExecutionMode executionMode)
    {
        this(executionMode, new SessionData());
    }

    /**
     * Constructs a RestApiSession with custom session data and mode.
     *
     * @param executionMode the mode governing session execution
     * @param sessionData the session variables mapping
     */
    RestApiSession(final ExecutionMode executionMode, final SessionData sessionData)
    {
        this(
            sessionData,
            createLlmRegistry(),
            new ExecutionEventBus(),
            new RestTargetExecutor(),
            executionMode
        );
    }

    private RestApiSession(
        final SessionData sessionData,
        final LlmRegistry llmRegistry,
        final ExecutionEventBus eventBus,
        final RestTargetExecutor targetExecutor,
        final ExecutionMode executionMode
    )
    {
        super(sessionData, llmRegistry, eventBus, targetExecutor, executionMode);
    }

    private static LlmRegistry createLlmRegistry()
    {
        final LlmRegistry registry = new LlmRegistry();
        final AiConfiguration config = new AiConfiguration();
        LlmRegistry.bootstrap(registry, config);
        return registry;
    }

    @Override
    public void close() throws Exception
    {
        if (getTargetExecutor() instanceof AutoCloseable closeable)
        {
            closeable.close();
        }
    }
}
