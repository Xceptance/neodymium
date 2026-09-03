/*
 * Copyright 2026 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neodymium.ai.resources;

// AI-generated: Gemini 3.5 Flash

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit and integration tests for {@link ClasspathResourceManager} validating
 * dual-write/delete classpath synchronization during local development.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ClasspathResourceManagerTest
{
    /**
     * Constructs a default ClasspathResourceManagerTest.
     */
    public ClasspathResourceManagerTest()
    {
    }

    /**
     * Verifies that writing and deleting files via {@link ClasspathResourceManager}
     * correctly synchronizes both compiled classpaths and source resources directory structures.
     *
     * @throws IOException if file operations fail
     */
    @Test
    public void testDualWriteAndClean() throws IOException
    {
        final ClasspathResourceManager manager = new ClasspathResourceManager();
        final String identifier = "playbooks/integration/test-temp-dualwrite-resource-delete-me.json";
        final String expectedContent = "{\"testKey\":\"testValue\"}";

        // 1. Write the resource
        manager.write(identifier, expectedContent);

        // 2. Resolve target path
        final URL rootUrl = Thread.currentThread().getContextClassLoader().getResource("");
        assertNotNull(rootUrl);
        
        final Path rootPath;
        try
        {
            rootPath = Path.of(rootUrl.toURI());
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to resolve classpath URI", e);
        }
        
        final Path targetPath = rootPath.resolve(identifier);
        
        // 3. Resolve source path based on target path translation
        Path srcRootPath = null;
        final String rootPathStr = rootPath.toString();
        if (rootPathStr.contains("target" + File.separator + "test-classes"))
        {
            srcRootPath = Path.of(rootPathStr.replace(
                "target" + File.separator + "test-classes",
                "src" + File.separator + "test" + File.separator + "resources"
            ));
        }
        else if (rootPathStr.contains("build" + File.separator + "classes" + File.separator + "java" + File.separator + "test"))
        {
            srcRootPath = Path.of(rootPathStr.replace(
                "build" + File.separator + "classes" + File.separator + "java" + File.separator + "test",
                "src" + File.separator + "test" + File.separator + "resources"
            ));
        }

        // Verify it was written to the compiled output directory
        assertTrue(Files.exists(targetPath), "Target path should exist: " + targetPath);

        // If we resolved a local src/test/resources directory, verify it was written there too
        if (srcRootPath != null && Files.isDirectory(srcRootPath))
        {
            final Path srcTargetPath = srcRootPath.resolve(identifier);
            assertTrue(Files.exists(srcTargetPath), "Source path should exist: " + srcTargetPath);
        }

        // 4. Read the file back via ClasspathResourceManager and verify the content matches
        try (final InputStream in = manager.read(identifier);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            final String result = reader.lines().collect(Collectors.joining("\n"));
            assertEquals(expectedContent, result);
        }

        // 5. Delete the file
        manager.delete(identifier);

        // Verify the file was deleted from both target and source resource folders
        assertFalse(Files.exists(targetPath), "Target path should be deleted");
        
        if (srcRootPath != null && Files.isDirectory(srcRootPath))
        {
            final Path srcTargetPath = srcRootPath.resolve(identifier);
            assertFalse(Files.exists(srcTargetPath), "Source path should be deleted");
        }
    }

    /**
     * Verifies that the path inclusion resolver handles relative path inclusion lookups correctly.
     */
    @Test
    public void testResolveInclude()
    {
        final ClasspathResourceManager manager = new ClasspathResourceManager();
        
        final String resolved = manager.resolveInclude("playbooks/integration/guest-checkout-verla.yaml", "../common/setup.yaml");
        assertEquals("playbooks/common/setup.yaml", resolved);
    }

    /**
     * Verifies that when reading a package-prefixed path that does not exist in the package folder,
     * {@link ClasspathResourceManager} successfully falls back to loading the file from the root resources.
     *
     * @throws IOException if file operations fail
     */
    @Test
    public void testFallbackToRootResource() throws IOException
    {
        final ClasspathResourceManager manager = new ClasspathResourceManager();
        final String rootIdentifier = "test-temp-fallback-root-resource-delete-me.yaml";
        final String packagePrefixedIdentifier = "com/example/tests/" + rootIdentifier;
        final String expectedContent = "key: fallbackValue";

        try
        {
            // 1. Write file directly to root resources identifier
            manager.write(rootIdentifier, expectedContent);

            // 2. Attempt to read via package-prefixed path
            try (final InputStream in = manager.read(packagePrefixedIdentifier);
                 final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                final String result = reader.lines().collect(Collectors.joining("\n"));
                assertEquals(expectedContent, result);
            }
        }
        finally
        {
            // Clean up created test file
            manager.delete(rootIdentifier);
        }
    }
}
