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
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.SessionData;

/**
 * Concrete package-private mock session implementation for testing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
final class MockSession extends AiSession
{
    /**
     * Flag tracking if close was invoked on this session.
     */
    private boolean closed = false;

    /**
     * Constructs a MockSession.
     */
    MockSession(
        final SessionData sessionData,
        final LlmRegistry llmRegistry,
        final ExecutionEventBus eventBus,
        final TargetExecutor targetExecutor
    )
    {
        super(sessionData, llmRegistry, eventBus, targetExecutor);
    }

    /**
     * Checks if this session is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed()
    {
        return this.closed;
    }

    /**
     * Closes the session.
     *
     * @throws Exception if closing fails
     */
    @Override
    public void close() throws Exception
    {
        this.closed = true;
    }
}
