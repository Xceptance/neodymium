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
package org.neodymium.ai.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test suite for {@link AtomicFileUtils}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AtomicFileUtilsTest
{
    @Test
    @DisplayName("Verify atomic string write creates file with expected content")
    public void testAtomicWrite(@TempDir final Path tempDir) throws IOException
    {
        final Path targetPath = tempDir.resolve("test-atomic.json");
        final String content = "{\"status\":\"running\",\"step\":1}";

        AtomicFileUtils.writeStringAtomic(targetPath, content);

        Assertions.assertTrue(Files.exists(targetPath), "File should exist after atomic write");
        final String readContent = Files.readString(targetPath, StandardCharsets.UTF_8);
        Assertions.assertEquals(content, readContent, "Content written atomically should match expected content");
    }

    @Test
    @DisplayName("Verify atomic string write with File overload works correctly")
    public void testAtomicWriteFileOverload(@TempDir final Path tempDir) throws IOException
    {
        final File targetFile = tempDir.resolve("test-file-overload.json").toFile();
        final String content = "{\"testName\":\"DemoTest\"}";

        AtomicFileUtils.writeStringAtomic(targetFile, content);

        Assertions.assertTrue(targetFile.exists(), "File should exist after atomic write");
        final String readContent = Files.readString(targetFile.toPath(), StandardCharsets.UTF_8);
        Assertions.assertEquals(content, readContent, "Content written atomically should match expected content");
    }

    @Test
    @DisplayName("Verify atomic overwrite updates file without leaving temp files behind")
    public void testAtomicOverwrite(@TempDir final Path tempDir) throws IOException
    {
        final Path targetPath = tempDir.resolve("test-overwrite.json");
        AtomicFileUtils.writeStringAtomic(targetPath, "{\"step\":1}");
        AtomicFileUtils.writeStringAtomic(targetPath, "{\"step\":2}");

        final String readContent = Files.readString(targetPath, StandardCharsets.UTF_8);
        Assertions.assertEquals("{\"step\":2}", readContent, "File should contain updated content");

        try (final var stream = Files.list(tempDir))
        {
            final long count = stream.count();
            Assertions.assertEquals(1, count, "Directory should only contain the final file and no leftover temp files");
        }
    }
}
