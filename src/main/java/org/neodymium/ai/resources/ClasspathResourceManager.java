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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * PlaybookResourceManager implementation that reads resources from the Java Classpath.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ClasspathResourceManager implements PlaybookResourceManager
{
    private final ClassLoader classLoader;

    /**
     * Constructs a ClasspathResourceManager using the thread context classloader.
     */
    public ClasspathResourceManager()
    {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Constructs a ClasspathResourceManager using the specified classloader.
     *
     * @param classLoader the class loader to load resources from
     */
    public ClasspathResourceManager(final ClassLoader classLoader)
    {
        this.classLoader = classLoader != null ? classLoader : getClass().getClassLoader();
    }

    @Override
    public InputStream read(final String identifier) throws IOException
    {
        final InputStream in = classLoader.getResourceAsStream(identifier);
        if (in == null)
        {
            throw new FileNotFoundException("Classpath resource not found: " + identifier);
        }
        return in;
    }

    @Override
    public void write(final String identifier, final String content) throws IOException
    {
        final java.net.URL rootUrl = classLoader.getResource("");
        if (rootUrl != null && "file".equals(rootUrl.getProtocol()))
        {
            try
            {
                final Path rootPath = Path.of(rootUrl.toURI());
                final Path targetPath = rootPath.resolve(identifier);
                java.nio.file.Files.createDirectories(targetPath.getParent());
                java.nio.file.Files.writeString(targetPath, content);
                return;
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to write resource to classpath output directory", e);
            }
        }
        throw new UnsupportedOperationException("ClasspathResourceManager is read-only when not running from local file system");
    }

    @Override
    public String resolveInclude(final String parentIdentifier, final String relativePath)
    {
        if (parentIdentifier == null || parentIdentifier.trim().isEmpty())
        {
            return Path.of(relativePath).normalize().toString().replace('\\', '/');
        }
        
        final Path parentPath = Path.of(parentIdentifier);
        final Path parentDir = parentPath.getParent();
        
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
