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
package org.neodymium.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResponseSchema}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class ResponseSchemaTest
{
    @Test
    public void testResolveMaxOutputTokens()
    {
        assertEquals(2048, ResponseSchema.resolveMaxOutputTokens(null));
        assertEquals(256, ResponseSchema.resolveMaxOutputTokens(ResponseSchema.STEP_SPLITS));
        assertEquals(1024, ResponseSchema.resolveMaxOutputTokens(ResponseSchema.JUDGE));
        assertEquals(4096, ResponseSchema.resolveMaxOutputTokens(ResponseSchema.ASSERTION));
        assertEquals(4096, ResponseSchema.resolveMaxOutputTokens(ResponseSchema.ACTIONS));
        assertEquals(2048, ResponseSchema.resolveMaxOutputTokens(ResponseSchema.TEXT));
    }

    @Test
    public void testResolveReasoningEffort()
    {
        assertEquals(ReasoningEffort.MEDIUM, ResponseSchema.resolveReasoningEffort(null));
        assertEquals(ReasoningEffort.LOW, ResponseSchema.resolveReasoningEffort(ResponseSchema.STEP_SPLITS));
        assertEquals(ReasoningEffort.MEDIUM, ResponseSchema.resolveReasoningEffort(ResponseSchema.JUDGE));
        assertEquals(ReasoningEffort.MEDIUM, ResponseSchema.resolveReasoningEffort(ResponseSchema.TEXT));
        assertEquals(ReasoningEffort.HIGH, ResponseSchema.resolveReasoningEffort(ResponseSchema.ACTIONS));
        assertEquals(ReasoningEffort.HIGH, ResponseSchema.resolveReasoningEffort(ResponseSchema.ASSERTION));
    }
}
