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
}
