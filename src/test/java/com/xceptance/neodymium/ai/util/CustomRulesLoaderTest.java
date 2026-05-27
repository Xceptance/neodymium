/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
 * // AI-generated: Gemini 3.5 Flash
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
        final String content = CustomRulesLoader.loadCustomRules("ai-prompts/pesap-classify-prompt.md");
        assertNotNull(content);
        assertTrue(content.contains("Pre-Execution Static Analysis"));
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
