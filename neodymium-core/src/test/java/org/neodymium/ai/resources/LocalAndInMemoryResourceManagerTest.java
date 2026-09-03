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
package org.neodymium.ai.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TDD validation tests for {@link LocalFileResourceManager} and {@link InMemoryResourceManager}.
 * Ensures disk IO operations, path normalization, and in-memory structures work correctly.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LocalAndInMemoryResourceManagerTest
{
    /**
     * Verifies that the LocalFileResourceManager can read, write, and resolve paths
     * on the local disk using Java NIO paths within a temp directory.
     *
     * @param tempDir the JUnit managed temporary directory
     * @throws IOException if any I/O operation fails
     */
    @Test
    public void testLocalFileResourceManager(final @TempDir Path tempDir) throws IOException
    {
        final PlaybookResourceManager manager = new LocalFileResourceManager(tempDir);

        final String content = "steps:\n  - Click button";
        final String relativeFilePath = "playbooks/login.yaml";

        // 1. Write content to relative file path
        manager.write(relativeFilePath, content);

        // Verify that the file was created on disk in the correct folder structure
        final Path targetFile = tempDir.resolve(relativeFilePath);
        assertTrue(Files.exists(targetFile));

        // 2. Read content back and verify
        try (final InputStream in = manager.read(relativeFilePath);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            final String result = reader.lines().collect(Collectors.joining("\n"));
            assertEquals(content, result);
        }

        // 3. Verify path inclusion resolution logic
        // parent is 'playbooks/login.yaml', relative is '../common/setup.yaml'
        // resolved path should normalize to 'common/setup.yaml'
        final String resolved = manager.resolveInclude("playbooks/login.yaml", "../common/setup.yaml");
        assertEquals("common/setup.yaml", resolved);

        // 4. Verify reading non-existent file throws exception
        assertThrows(IOException.class, () -> {
            manager.read("nonexistent.yaml");
        });
    }

    /**
     * Verifies that the InMemoryResourceManager reads and writes content correctly
     * using thread-safe map structures without touching the filesystem.
     *
     * @throws IOException if any I/O operation fails
     */
    @Test
    public void testInMemoryResourceManager() throws IOException
    {
        final PlaybookResourceManager manager = new InMemoryResourceManager();

        final String content = "steps:\n  - Navigate to home";
        final String path = "playbooks/home.yaml";

        // 1. Write content to in-memory store
        manager.write(path, content);

        // 2. Read content back and verify
        try (final InputStream in = manager.read(path);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            final String result = reader.lines().collect(Collectors.joining("\n"));
            assertEquals(content, result);
        }

        // 3. Verify in-memory inclusion path resolution
        final String resolved = manager.resolveInclude("playbooks/home.yaml", "includes/header.yaml");
        assertEquals("playbooks/includes/header.yaml", resolved);

        // 4. Verify reading non-existent key throws exception
        assertThrows(IOException.class, () -> {
            manager.read("missing.yaml");
        });
    }
}
