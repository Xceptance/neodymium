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
package org.neodymium.ai.executor.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.ContextLevel;

/**
 * Unit tests for {@link RestTargetExecutor}.
 * Validates authentication header registration and SUT state capture.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class RestTargetExecutorTest
{
    @Test
    public void testRestTargetExecutorStateCapture() throws Exception
    {
        final RestTargetExecutor executor = new RestTargetExecutor();
        executor.registerBearerToken("mock-token");
        executor.registerBasicAuth("user", "pass");

        final SutState state = executor.captureState(ContextLevel.STANDARD);

        assertNotNull(state, "Captured SUT state should not be null.");
        assertNotNull(state.getTextContent(), "Text content should not be null.");
    }
}
