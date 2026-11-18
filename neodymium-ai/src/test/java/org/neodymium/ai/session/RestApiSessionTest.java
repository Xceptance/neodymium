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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.rest.RestTargetExecutor;

/**
 * Unit tests for {@link RestApiSession}.
 * Validates REST API session initialization and lifecycle.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class RestApiSessionTest
{
    @Test
    public void testRestApiSessionInitialization() throws Exception
    {
        try (final RestApiSession session = new RestApiSession(ExecutionMode.LLM_ONLY))
        {
            assertNotNull(session.getTargetExecutor(), "Target executor should not be null.");
            assertEquals(RestTargetExecutor.class, session.getTargetExecutor().getClass(), "Target executor should be RestTargetExecutor.");
            assertEquals(ExecutionMode.LLM_ONLY, session.getExecutionMode(), "Execution mode should match.");
        }
    }
}
