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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests validating that {@link VerificationResult} correctly handles
 * the new rubric-based AI Judge JSON structure.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerificationResultTest
{
    /**
     * Default constructor for test suite execution.
     */
    public VerificationResultTest()
    {
    }

    /**
     * Verifies parsing new rubric-based JSON structure correctly maps to the nested classes.
     *
     * @throws Exception if deserialization fails
     */
    @Test
    public void testRubricsJsonMapping() throws Exception
    {
        final String rubricJson = """
            {
              "rubrics": {
                "intentMatch": {
                  "analysis": "Intent verified successfully",
                  "score": "PASS"
                },
                "visualDelta": {
                  "analysis": "Visual transition confirmed",
                  "score": "PASS"
                },
                "absenceOfErrors": {
                  "analysis": "Errors detected on page",
                  "score": "FAIL"
                }
              },
              "overallVerdict": {
                "passed": false,
                "summary": "Failed validation due to page errors"
              }
            }
            """;

        final VerificationResult result = ResponseRepairService.deserialize(rubricJson, VerificationResult.class);

        assertNotNull(result);
        assertFalse(result.passed());
        assertEquals("Intent verified successfully [Score: PASS]", result.actionReasoning());
        assertEquals("Visual transition confirmed [Score: PASS] | Error Check: Errors detected on page [Score: FAIL]", result.visualReasoning());

        assertNotNull(result.getRubrics());
        assertNotNull(result.getOverallVerdict());
        assertEquals("PASS", result.getRubrics().intentMatch().score());
        assertEquals("PASS", result.getRubrics().visualDelta().score());
        assertEquals("FAIL", result.getRubrics().absenceOfErrors().score());
        assertEquals("Failed validation due to page errors", result.getOverallVerdict().summary());
    }
}
