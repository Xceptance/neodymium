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
package com.xceptance.neodymium.ai.console;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * Tests for structured run and test class folder generation in InteractiveConsoleEngine.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class StructuredFolderExecutionTest
{
    @Test
    public void testGetRunFolderIsNotNull()
    {
        final String runFolder = InteractiveConsoleEngine.getRunFolder();
        assertNotNull(runFolder, "Run folder should not be null");
        assertTrue(runFolder.startsWith("run_") || runFolder.startsWith("run-"), "Run folder should start with run_ or run-");
    }

    @Test
    public void testExtractTestClassFolderFromTestFile()
    {
        final JsonObject json = new JsonObject();
        json.addProperty("testFile", "com.xceptance.neodymium.test.examples.WikipediaSearchTest#executeWikipediaSearch");

        final String folder = InteractiveConsoleEngine.extractTestClassFolder(json);
        assertTrue("WikipediaSearchTest".equals(folder), "Extracted test class folder should be WikipediaSearchTest, got: " + folder);
    }

    @Test
    public void testExtractTestClassFolderFromTestName()
    {
        final JsonObject json = new JsonObject();
        json.addProperty("testName", "WikipediaSearchTest · dataset_1");

        final String folder = InteractiveConsoleEngine.extractTestClassFolder(json);
        assertTrue("WikipediaSearchTest".equals(folder), "Extracted test class folder should be WikipediaSearchTest, got: " + folder);
    }
}
