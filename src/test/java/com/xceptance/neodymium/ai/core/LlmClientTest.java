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

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link LlmClient} masking utilities.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
class LlmClientTest
{
    @Test
    void maskKey_withBlankKeys_returnsEmptyString()
    {
        assertEquals("", LlmClient.maskKey(null));
        assertEquals("", LlmClient.maskKey(""));
        assertEquals("", LlmClient.maskKey("   "));
    }

    @Test
    void maskKey_withShortKeys_returnsFullyMaskedKeysWithCorrectLength()
    {
        assertEquals("*", LlmClient.maskKey("a"));
        assertEquals("****", LlmClient.maskKey("1234"));
        assertEquals("********", LlmClient.maskKey("12345678"));
    }

    @Test
    void maskKey_withLongKeys_returnsMaskedMiddleKeysWithCorrectLength()
    {
        // Length 9 key
        final String key9 = "123456789";
        final String masked9 = LlmClient.maskKey(key9);
        assertEquals("1234*6789", masked9);
        assertEquals(key9.length(), masked9.length());

        // Length 10 key
        final String key10 = "1234567890";
        final String masked10 = LlmClient.maskKey(key10);
        assertEquals("1234**7890", masked10);
        assertEquals(key10.length(), masked10.length());

        // Real-world length key (42 characters)
        final String realKey = "fake-key-1234567890-abcdef-1234567890-abcd";
        final String maskedReal = LlmClient.maskKey(realKey);
        assertEquals(realKey.length(), maskedReal.length());
        assertEquals("fake", maskedReal.substring(0, 4));
        assertEquals("abcd", maskedReal.substring(realKey.length() - 4));
        assertEquals("*".repeat(realKey.length() - 8), maskedReal.substring(4, realKey.length() - 4));
    }
}
