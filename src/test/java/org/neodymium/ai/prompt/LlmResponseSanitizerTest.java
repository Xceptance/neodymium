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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LlmResponseSanitizer}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmResponseSanitizerTest
{
    @Test
    public void testCleanJsonObject()
    {
        final String input = "{\"status\": \"SUCCESS\"}";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("{\"status\": \"SUCCESS\"}", actual);
    }

    @Test
    public void testMarkdownWrappedJson()
    {
        final String input = "```json\n{\"status\": \"SUCCESS\"}\n```";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("{\"status\": \"SUCCESS\"}", actual);
    }

    @Test
    public void testMalformedLeadingLabelHeader()
    {
        final String input = "{\\label} : country_selector_click\n{\n  \"status\": \"SUCCESS\"\n}";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("{\n  \"status\": \"SUCCESS\"\n}", actual);
    }

    @Test
    public void testJsonArray()
    {
        final String input = "Here is the result:\n[1, 2, 3]";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("[1, 2, 3]", actual);
    }

    @Test
    public void testNullOrBlank()
    {
        Assertions.assertEquals("", LlmResponseSanitizer.extractJson(null));
        Assertions.assertEquals("", LlmResponseSanitizer.extractJson("   "));
    }
}
