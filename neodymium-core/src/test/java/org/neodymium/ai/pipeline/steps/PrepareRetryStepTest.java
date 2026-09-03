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

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Unit test suite for {@link PrepareRetryStep}.
 * Tests graceful handling when WebDriver session is inactive.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class PrepareRetryStepTest
{
    /**
     * Goal: Verifies that {@link PrepareRetryStep#execute} completes safely without throwing any exceptions
     * when executed in an environment where no active WebDriver instance is initialized.
     */
    @Test
    public void testPrepareRetryExecutesWithoutWebDriverStarted()
    {
        final ExecutionContext context = new ExecutionContext(new SessionData(new HashMap<>()));
        final PrepareRetryStep step = new PrepareRetryStep();

        // Assert step handles inactive WebDriver state gracefully without crashing
        Assertions.assertDoesNotThrow(() ->
        {
            step.execute(context);
        });
    }
}
