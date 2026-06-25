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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.BaseAiOfflineTest;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Tests for filtering out unexecuted steps in step execution statistics.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiStepStatsLoggingTest extends BaseAiOfflineTest
{
    @Test
    public final void testAllStepsExecutedOnSuccess()
    {
        final AiMockResponse mockRes = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .build();
        this.llmClient.addResponse(mockRes);
        this.llmClient.addResponse(mockRes);

        final AiExecutionResult result = this.mockBrowser.execute("Click button 1\nClick button 2");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSteps().size());
        assertTrue(result.getSteps().get(0).isExecuted());
        assertTrue(result.getSteps().get(1).isExecuted());
    }

    @Test
    public final void testUnexecutedStepsNotMarkedExecutedOnFailure()
    {
        final AiMockResponse mockRes = AiMockResponse.builder()
                .httpStatusCode(500)
                .build();
        this.llmClient.addResponse(mockRes);

        assertThrows(Throwable.class, () -> {
            this.mockBrowser.execute("Click button 1\nClick button 2");
        });

        final AiExecutionResult result = Neodymium.getLastAiExecutionResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(2, result.getSteps().size());
        assertTrue(result.getSteps().get(0).isExecuted());
        assertFalse(result.getSteps().get(1).isExecuted());
    }
}
