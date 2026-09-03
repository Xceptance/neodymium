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
 * Unit tests for {@link LlmRequest} settings validation.
 * Ensures request temperature and timeout values are correctly structured.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class LlmProviderSettingsTest
{
    @Test
    public void testLlmRequestCarriesSettings()
    {
        final LlmRequest request = new LlmRequest("system", "user", java.util.Collections.emptyList(), ResponseSchema.TEXT, 0.7, 45);
        assertEquals(0.7, request.temperature(), 0.001, "Temperature should be stored on request.");
        assertEquals(45, request.timeoutSeconds(), "Timeout seconds should be stored on request.");
    }
}
