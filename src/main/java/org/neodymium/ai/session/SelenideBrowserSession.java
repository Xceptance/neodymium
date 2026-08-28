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
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.SessionData;

/**
 * Package-private {@link AiSession} implementation tailored for browser automation
 * backed by {@link SelenideTargetExecutor}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
class SelenideBrowserSession extends AiSession
{
    /**
     * Constructs a SelenideBrowserSession with default session data.
     *
     * @param executionMode the mode governing session execution
     */
    SelenideBrowserSession(final ExecutionMode executionMode)
    {
        this(executionMode, new SessionData());
    }

    /**
     * Constructs a SelenideBrowserSession with custom session data and mode.
     *
     * @param executionMode the mode governing session execution
     * @param sessionData the session variables mapping
     */
    SelenideBrowserSession(final ExecutionMode executionMode, final SessionData sessionData)
    {
        this(
            sessionData,
            createLlmRegistry(),
            new ExecutionEventBus(),
            new SelenideTargetExecutor(),
            executionMode
        );
    }

    private SelenideBrowserSession(
        final SessionData sessionData,
        final LlmRegistry llmRegistry,
        final ExecutionEventBus eventBus,
        final SelenideTargetExecutor targetExecutor,
        final ExecutionMode executionMode
    )
    {
        super(sessionData, llmRegistry, eventBus, targetExecutor, executionMode);
        targetExecutor.setExecutionContext(getExecutionContext());
    }

    private static LlmRegistry createLlmRegistry()
    {
        final LlmRegistry registry = new LlmRegistry();
        final AiConfiguration config = AiConfiguration.getInstance();
        LlmRegistry.bootstrap(registry, config);
        org.neodymium.ai.client.LlmCacheHelper.wrapRegistryIfActive(registry);
        return registry;
    }

    @Override
    public void close() throws Exception
    {
        // Cleanup resources bound to target executor if AutoCloseable
        if (getTargetExecutor() instanceof AutoCloseable closeable)
        {
            closeable.close();
        }
    }
}
