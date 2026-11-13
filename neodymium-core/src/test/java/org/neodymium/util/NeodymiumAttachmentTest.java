/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package org.neodymium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.model.ExecutionAttachment;

/**
 * Unit tests for {@link Neodymium#addAttachment} and attachment lifecycle.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAttachmentTest
{
    @BeforeEach
    @AfterEach
    public void cleanup()
    {
        Neodymium.clearAttachments();
    }

    @Test
    public void testAddFileAttachment(@TempDir final Path tempDir) throws IOException
    {
        final Path sampleFile = tempDir.resolve("sample.csv");
        Files.writeString(sampleFile, "col1,col2\nval1,val2\n");

        Neodymium.addAttachment("sample.csv", sampleFile.toFile(), false);

        final List<ExecutionAttachment> attachments = Neodymium.getAttachments();
        assertEquals(1, attachments.size());
        final ExecutionAttachment att = attachments.get(0);
        assertEquals("sample.csv", att.getName());
        assertEquals("sample.csv", att.getPath());
        assertFalse(att.isOpenByDefault());
        assertNotNull(att.getSourceFile());
        assertTrue(att.getSourceFile().exists());
    }

    @Test
    public void testAddTextAttachment()
    {
        final String jsonContent = "{\"key\":\"value\"}";
        Neodymium.addAttachment("data.json", jsonContent, "application/json", true);

        final List<ExecutionAttachment> attachments = Neodymium.getAttachments();
        assertEquals(1, attachments.size());
        final ExecutionAttachment att = attachments.get(0);
        assertEquals("data.json", att.getName());
        assertEquals("data.json", att.getPath());
        assertEquals("json", att.getType());
        assertEquals("application/json", att.getContentType());
        assertTrue(att.isOpenByDefault());
        assertEquals(jsonContent, att.getTextContent());
    }

    @Test
    public void testClearAttachments()
    {
        Neodymium.addAttachment("test.txt", "hello", "text/plain", false);
        assertEquals(1, Neodymium.getAttachments().size());

        Neodymium.clearAttachments();
        assertTrue(Neodymium.getAttachments().isEmpty());
    }

    @Test
    public void testDeduplicationOnSameName()
    {
        Neodymium.addAttachment("state.json", "{\"v\":1}", "application/json", false);
        assertEquals(1, Neodymium.getAttachments().size());
        assertEquals("{\"v\":1}", Neodymium.getAttachments().get(0).getTextContent());
        assertFalse(Neodymium.getAttachments().get(0).isOpenByDefault());

        // Update same attachment with different content and openByDefault=true
        Neodymium.addAttachment("state.json", "{\"v\":2}", "application/json", true);
        assertEquals(1, Neodymium.getAttachments().size(), "Attachments with identical name must replace rather than duplicate");
        assertEquals("{\"v\":2}", Neodymium.getAttachments().get(0).getTextContent());
        assertTrue(Neodymium.getAttachments().get(0).isOpenByDefault());
    }

    @Test
    public void testClearThreadContextPurgesAttachments()
    {
        Neodymium.addAttachment("temp.log", "some-log", "text/plain", false);
        assertFalse(Neodymium.getAttachments().isEmpty());

        Neodymium.clearThreadContext();
        assertTrue(Neodymium.getAttachments().isEmpty(), "clearThreadContext must purge all attachments");
    }

    @Test
    public void testPathSanitization()
    {
        // 1. Relative subpaths use underscore
        assertEquals("sub_dir_data.json", ExecutionAttachment.sanitizeAttachmentPath("sub/dir/data.json"));
        assertEquals("before_screen.png", ExecutionAttachment.sanitizeAttachmentPath("before\\screen.png"));

        // 2. Absolute paths reduce to basename
        assertEquals("secret.pem", ExecutionAttachment.sanitizeAttachmentPath("/home/user/keys/secret.pem"));
        assertEquals("output.txt", ExecutionAttachment.sanitizeAttachmentPath("C:\\Windows\\Temp\\output.txt"));

        // 3. Traversal dots and illegal characters stripped
        assertEquals("passwd", ExecutionAttachment.sanitizeAttachmentPath("../../../passwd"));
        assertEquals("data_v=1.json", ExecutionAttachment.sanitizeAttachmentPath("data?v=1.json"));
        assertEquals("report_hash.html", ExecutionAttachment.sanitizeAttachmentPath("report#hash.html"));

        // 4. Dotfiles prefixed to avoid hidden files
        assertEquals("_env", ExecutionAttachment.sanitizeAttachmentPath(".env"));

        // 5. Long stem capped while preserving extension
        final String longName = "a".repeat(120) + ".json";
        final String sanitized = ExecutionAttachment.sanitizeAttachmentPath(longName);
        assertTrue(sanitized.endsWith(".json"), "Extension must be preserved when capping filename");
        assertTrue(sanitized.length() <= 86, "Stem must be capped to prevent exceeding filesystem limits");
    }
}
