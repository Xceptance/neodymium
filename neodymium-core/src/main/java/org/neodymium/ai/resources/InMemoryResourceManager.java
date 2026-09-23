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
    private final PlaybookResourceManager delegate;

    /**
     * Constructs a default InMemoryResourceManager with ClasspathResourceManager fallback.
     */
    public InMemoryResourceManager()
    {
        this(new ClasspathResourceManager());
    }

    /**
     * Constructs an InMemoryResourceManager with a fallback delegate resource manager.
     *
     * @param delegate the fallback resource manager
     */
    public InMemoryResourceManager(final PlaybookResourceManager delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Opens an input stream to read a resource's raw content from memory or fallback delegate.
     *
     * @param identifier the resource key
     * @return the input stream to read the resource content
     * @throws IOException if the resource is not found
     */
    @Override
    public InputStream read(final String identifier) throws IOException
    {
        final String content = this.storage.get(identifier);
        if (content != null)
        {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
        if (this.delegate != null)
        {
            try
            {
                return this.delegate.read(identifier);
            }
            catch (final Exception ignored)
            {
                // Fall through to throw exception below
            }
        }
        throw new FileNotFoundException("Resource not found in memory or delegate: " + identifier);
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

    @Override
    public void delete(final String identifier) throws IOException
    {
        this.storage.remove(identifier);
    }

    /**
     * Resolves a relative resource inclusion path against a parent resource path.
     *
     * @param parentIdentifier the identifier of the parent resource
     * @param relativePath the relative path to resolve
     * @return the normalized relative path string
     */
    public boolean hasResource(final String identifier)
    {
        if (identifier == null || identifier.trim().isEmpty())
        {
            return false;
        }
        final String norm = Path.of(identifier).normalize().toString().replace('\\', '/');
        if (this.storage.containsKey(norm) || this.storage.containsKey(identifier))
        {
            return true;
        }
        if (this.delegate != null)
        {
            try (final InputStream in = this.delegate.read(identifier))
            {
                return in != null;
            }
            catch (final Exception e)
            {
                return false;
            }
        }
        return false;
    }

    @Override
    public String resolveInclude(final String parentIdentifier, final String relativePath)
    {
        if (relativePath == null || relativePath.trim().isEmpty())
        {
            return relativePath;
        }
        final String cleanRelative = relativePath.trim().replace('\\', '/');

        if (parentIdentifier == null || parentIdentifier.trim().isEmpty() || cleanRelative.startsWith("/"))
        {
            return Path.of(cleanRelative).normalize().toString().replace('\\', '/');
        }

        final Path parentPath = Path.of(parentIdentifier.trim().replace('\\', '/'));
        final Path parentDir = parentPath.getParent();

        if (parentDir != null)
        {
            Path currentDir = parentDir;
            while (currentDir != null)
            {
                final String candidate = currentDir.resolve(cleanRelative).normalize().toString().replace('\\', '/');
                if (hasResource(candidate))
                {
                    return candidate;
                }
                currentDir = currentDir.getParent();
            }
        }

        final String normalizedRelative = Path.of(cleanRelative).normalize().toString().replace('\\', '/');
        if (hasResource(normalizedRelative))
        {
            return normalizedRelative;
        }

        if (parentDir != null)
        {
            return parentDir.resolve(cleanRelative).normalize().toString().replace('\\', '/');
        }

        return normalizedRelative;
    }
}
