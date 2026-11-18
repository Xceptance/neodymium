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
package org.neodymium.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResponseRepairService}.
 * Validates extraction and repair of JSON responses wrapped in markdown code fences,
 * leading/trailing conversational text, and missing structural closing braces.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ResponseRepairServiceTest
{
    @Test
    public void testRepairMarkdownCodeFence()
    {
        final String rawLlmOutput = """
            Here is the requested json response:
            ```json
            {
              "status": "SUCCESS",
              "action": "CLICK"
            }
            ```
            Hope this helps!
            """;

        final String repairedJson = ResponseRepairService.repairJson(rawLlmOutput);
        assertEquals("{\n  \"status\": \"SUCCESS\",\n  \"action\": \"CLICK\"\n}", repairedJson, "Should strip markdown ```json fences and extra text.");
    }

    @Test
    public void testRepairUnbalancedBraces()
    {
        final String truncatedLlmOutput = "{\n  \"status\": \"SUCCESS\",\n  \"message\": \"Truncated";

        final String repairedJson = ResponseRepairService.repairJson(truncatedLlmOutput);
        assertEquals("{\n  \"status\": \"SUCCESS\",\n  \"message\": \"Truncated}", repairedJson, "Should append missing closing brace }.");
    }

    @Test
    public void testRepairPlainJsonWithNoMarkdownFences()
    {
        final String plainJson = "{\"key\": \"value\"}";
        final String repaired = ResponseRepairService.repairJson(plainJson);
        assertEquals(plainJson, repaired, "Clean JSON string should be returned unchanged.");
    }
}
