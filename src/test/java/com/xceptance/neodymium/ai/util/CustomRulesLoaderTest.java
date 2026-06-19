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
package com.xceptance.neodymium.ai.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CustomRulesLoader verifying classpath, filesystem, fallback,
 * and thread-local override resolution logic.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
final class CustomRulesLoaderTest
{
    private File tempFile;

    @BeforeEach
    void setUp()
    {
        Neodymium.getData().clear();
    }

    @AfterEach
    void tearDown()
    {
        Neodymium.getData().clear();
        if (tempFile != null && tempFile.exists())
        {
            tempFile.delete();
        }
    }

    @Test
    void testLoadCustomRules_emptyAndNoFallbacks()
    {
        // Ensure no default filesystem fallback exists to verify clean empty path
        final File fallbackFile = new File("config/pesap-custom-rules.md");
        final File fallbackFileTxt = new File("config/pesap-custom-rules.txt");
        if (!fallbackFile.exists() && !fallbackFileTxt.exists())
        {
            final String content = CustomRulesLoader.loadCustomRules(null);
            assertEquals("", content);
        }
    }

    @Test
    void testLoadCustomRules_configuredClasspathFile()
    {
        // Use an existing test resource on the classpath
        final String content = CustomRulesLoader.loadCustomRules("ai-prompts/pesap-pre-step-prompt.md");
        assertNotNull(content);
        assertTrue(content.contains("Predict minimal DOM context level"));
    }

    @Test
    void testLoadCustomRules_configuredFilesystemFile() throws Exception
    {
        tempFile = File.createTempFile("pesap-custom-rules-test", ".md");
        final String expectedContent = "### Custom Rule\nVerify something specific.";
        Files.writeString(tempFile.toPath(), expectedContent);

        final String content = CustomRulesLoader.loadCustomRules(tempFile.getAbsolutePath());
        assertEquals(expectedContent, content);
    }

    @Test
    void testLoadCustomRules_configuredButDoesNotExist()
    {
        assertThrows(IllegalStateException.class, () -> {
            CustomRulesLoader.loadCustomRules("this/file/does/not/exist/anywhere.md");
        });
    }

    @Test
    void testLoadCustomRules_threadLocalOverride() throws Exception
    {
        tempFile = File.createTempFile("pesap-custom-rules-override", ".md");
        final String expectedContent = "### Custom Thread Local Rule\nOverride rule.";
        Files.writeString(tempFile.toPath(), expectedContent);

        // Put override path into thread local data
        Neodymium.getData().put("neodymium.ai.pesap.custom.file", tempFile.getAbsolutePath());

        // Pass a dummy property value, but thread local target should override it
        final String content = CustomRulesLoader.loadCustomRules("some-other-file.md");
        assertEquals(expectedContent, content);
    }

    @Test
    void testLoadCustomRules_fallbackPrecedence() throws Exception
    {
        final File fallbackDir = new File("config");
        boolean createdDir = false;
        if (!fallbackDir.exists())
        {
            fallbackDir.mkdirs();
            createdDir = true;
        }

        final File fallbackFile = new File("config/pesap-custom-rules.md");
        try
        {
            final String expectedContent = "### Default FS Fallback\nRule text.";
            Files.writeString(fallbackFile.toPath(), expectedContent);

            // When no config is provided, it should resolve to the filesystem fallback
            final String content = CustomRulesLoader.loadCustomRules(null);
            assertEquals(expectedContent, content);
        }
        finally
        {
            if (fallbackFile.exists())
            {
                fallbackFile.delete();
            }
            if (createdDir && fallbackDir.exists())
            {
                fallbackDir.delete();
            }
        }
    }
}
