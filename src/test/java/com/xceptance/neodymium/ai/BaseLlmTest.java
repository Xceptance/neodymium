/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai;

import org.junit.jupiter.api.BeforeAll;

/**
 * Lightweight, browserless base class for LLM-based integration tests.
 * Automatically resolves the Gemini API key from the environment before tests run.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public abstract class BaseLlmTest
{
    /**
     * Resolves the Gemini API key from the environment and registers it
     * as a system property before any tests run.
     */
    @BeforeAll
    public static void setUpApiKey()
    {
        final String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty() && System.getProperty("neodymium.ai.apiKey") == null)
        {
            System.setProperty("neodymium.ai.apiKey", envKey.trim());
        }
    }
}
