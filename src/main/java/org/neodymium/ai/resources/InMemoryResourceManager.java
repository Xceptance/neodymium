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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of {@link PlaybookResourceManager} that reads,
 * writes, and resolves relative files in-memory using thread-safe map storage.
 * Useful for fast unit testing and mock playbacks.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class InMemoryResourceManager implements PlaybookResourceManager
{
    /**
     * Thread-safe memory storage mapping resource identifiers to their raw string content payloads.
     */
    private final Map<String, String> storage = new ConcurrentHashMap<>();

    /**
     * Constructs a default InMemoryResourceManager.
     */
    public InMemoryResourceManager()
    {
    }

    /**
     * Opens an input stream to read a resource's raw content from the in-memory map.
     *
     * @param identifier the resource key
     * @return the input stream to read the resource content
     * @throws IOException if the resource is not found in the storage map
     */
    @Override
    public InputStream read(final String identifier) throws IOException
    {
        final String content = this.storage.get(identifier);
        if (content == null)
        {
            throw new FileNotFoundException("Resource not found in memory: " + identifier);
        }
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Writes raw content to the in-memory map.
     *
     * @param identifier the resource key
     * @param content the content payload to write
     * @throws IOException if writing content fails
     */
    @Override
    public void write(final String identifier, final String content) throws IOException
    {
        this.storage.put(identifier, content);
    }

    /**
     * Resolves a relative resource inclusion path against a parent resource path.
     * Normalizes the result and returns a string with standardized forward slashes.
     *
     * @param parentIdentifier the identifier of the parent resource
     * @param relativePath the relative path to resolve
     * @return the normalized relative path string
     */
    @Override
    public String resolveInclude(final String parentIdentifier, final String relativePath)
    {
        if (parentIdentifier == null || parentIdentifier.isEmpty())
        {
            return relativePath;
        }
        
        final Path parent = Path.of(parentIdentifier);
        final Path parentDir = parent.getParent();
        
        final Path resolved;
        if (parentDir == null)
        {
            resolved = Path.of(relativePath).normalize();
        }
        else
        {
            resolved = parentDir.resolve(relativePath).normalize();
        }
        
        return resolved.toString().replace('\\', '/');
    }
}
