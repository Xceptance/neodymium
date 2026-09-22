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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.neodymium.util.Neodymium;

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
        final String normalized = identifier != null && identifier.startsWith("/") ? identifier.substring(1) : identifier;
        // First check direct filesystem path if specified
        if (identifier != null)
        {
            final Path directPath = Path.of(identifier);
            if (Files.exists(directPath) && !Files.isDirectory(directPath))
            {
                return Files.newInputStream(directPath);
            }
        }

        InputStream in = classLoader.getResourceAsStream(normalized);
        if (in == null && !normalized.startsWith("ai-playbooks/"))
        {
            in = classLoader.getResourceAsStream("ai-playbooks/" + normalized);
        }
        if (in == null && normalized != null && normalized.contains("/"))
        {
            final String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
            if (!fileName.isEmpty())
            {
                in = classLoader.getResourceAsStream(fileName);
                if (in == null && !fileName.startsWith("ai-playbooks/"))
                {
                    in = classLoader.getResourceAsStream("ai-playbooks/" + fileName);
                }
            }
        }
        if (in != null)
        {
            return in;
        }

        final URL rootUrl = classLoader.getResource("");
        if (rootUrl != null && "file".equals(rootUrl.getProtocol()))
        {
            try
            {
                final Path rootPath = Path.of(rootUrl.toURI());
                final List<Path> candidatePaths = new ArrayList<>();
                candidatePaths.add(rootPath.resolve(normalized));
                candidatePaths.add(rootPath.resolve("ai-playbooks/" + normalized));

                final Path srcRoot = getSourceResourcesRoot();
                if (srcRoot != null)
                {
                    candidatePaths.add(srcRoot.resolve(normalized));
                    candidatePaths.add(srcRoot.resolve("ai-playbooks/" + normalized));
                    if (srcRoot.getParent() != null)
                    {
                        candidatePaths.add(srcRoot.getParent().resolve(normalized));
                        candidatePaths.add(srcRoot.getParent().resolve("ai-playbooks/" + normalized));
                        if (srcRoot.getParent().getParent() != null)
                        {
                            candidatePaths.add(srcRoot.getParent().getParent().resolve(normalized));
                        }
                    }
                }

                for (final Path candidate : candidatePaths)
                {
                    if (Files.exists(candidate) && !Files.isDirectory(candidate))
                    {
                        return Files.newInputStream(candidate);
                    }
                }
            }
            catch (final Exception e)
            {
                // ignore and fall through
            }
        }
        throw new FileNotFoundException("Classpath resource not found: " + identifier);
    }

    @Override
    public void write(final String identifier, final String content) throws IOException
    {
        if (identifier != null && (identifier.startsWith("target/") || identifier.startsWith("./target/") || Path.of(identifier).isAbsolute()))
        {
            try
            {
                final Path directTargetPath = Path.of(identifier);
                if (directTargetPath.getParent() != null)
                {
                    Files.createDirectories(directTargetPath.getParent());
                }
                Files.writeString(directTargetPath, content);
                return;
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to write resource to direct path: " + identifier, e);
            }
        }

        final URL rootUrl = classLoader.getResource("");
        if (rootUrl != null && "file".equals(rootUrl.getProtocol()))
        {
            try
            {
                final Path rootPath = Path.of(rootUrl.toURI());
                final Path targetPath = rootPath.resolve(identifier);
                Files.createDirectories(targetPath.getParent());
                Files.writeString(targetPath, content);

                // Also write to src/test/resources if running in local development mode and not targeting a target directory
                if (identifier != null && !identifier.startsWith("target/"))
                {
                    final Path srcRoot = getSourceResourcesRoot();
                    if (srcRoot != null)
                    {
                        final Path srcTargetPath = srcRoot.resolve(identifier);
                        Files.createDirectories(srcTargetPath.getParent());
                        Files.writeString(srcTargetPath, content);
                    }
                }
                return;
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to write resource to output directory", e);
            }
        }
        throw new UnsupportedOperationException("ClasspathResourceManager is read-only when not running from local file system");
    }

    @Override
    public void delete(final String identifier) throws IOException
    {
        if (identifier != null && (identifier.startsWith("target/") || identifier.startsWith("./target/") || Path.of(identifier).isAbsolute()))
        {
            try
            {
                final Path directTargetPath = Path.of(identifier);
                Files.deleteIfExists(directTargetPath);
                return;
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to delete resource from direct path: " + identifier, e);
            }
        }

        final URL rootUrl = classLoader.getResource("");
        if (rootUrl != null && "file".equals(rootUrl.getProtocol()))
        {
            try
            {
                final Path rootPath = Path.of(rootUrl.toURI());
                final Path targetPath = rootPath.resolve(identifier);
                Files.deleteIfExists(targetPath);

                // Also delete from src/test/resources if running in local development mode
                final Path srcRoot = getSourceResourcesRoot();
                if (srcRoot != null)
                {
                    final Path srcTargetPath = srcRoot.resolve(identifier);
                    Files.deleteIfExists(srcTargetPath);
                }
                return;
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to delete resource from classpath output directory", e);
            }
        }
        throw new UnsupportedOperationException("ClasspathResourceManager is read-only when not running from local file system");
    }

    /**
     * Resolves the project's source test resources root directory dynamically.
     * In multi-module environments, derives the submodule source path from the active classloader root.
     *
     * @return the source resources root path, or null if it cannot be resolved or does not exist
     */
    Path getSourceResourcesRoot()
    {
        // 1. Try deriving from active classLoader output root (target/test-classes, build/classes, bin)
        final URL rootUrl = this.classLoader.getResource("");
        if (rootUrl != null && "file".equals(rootUrl.getProtocol()))
        {
            try
            {
                final Path rootPath = Path.of(rootUrl.toURI());
                final String rootPathStr = rootPath.toString();

                if (rootPathStr.contains("target" + File.separator + "test-classes"))
                {
                    final Path candidate = Path.of(rootPathStr.replace(
                        "target" + File.separator + "test-classes",
                        "src" + File.separator + "test" + File.separator + "resources"
                    ));
                    if (Files.isDirectory(candidate))
                    {
                        return candidate;
                    }
                }
                if (rootPathStr.contains("target" + File.separator + "classes"))
                {
                    final Path candidate = Path.of(rootPathStr.replace(
                        "target" + File.separator + "classes",
                        "src" + File.separator + "main" + File.separator + "resources"
                    ));
                    if (Files.isDirectory(candidate))
                    {
                        return candidate;
                    }
                }
                if (rootPathStr.contains("build" + File.separator + "classes" + File.separator + "java" + File.separator + "test"))
                {
                    final Path candidate = Path.of(rootPathStr.replace(
                        "build" + File.separator + "classes" + File.separator + "java" + File.separator + "test",
                        "src" + File.separator + "test" + File.separator + "resources"
                    ));
                    if (Files.isDirectory(candidate))
                    {
                        return candidate;
                    }
                }
                // Traverse ancestor directories to locate submodule src/test/resources (e.g. under Eclipse /bin)
                Path current = rootPath;
                while (current != null && current.getParent() != null)
                {
                    final Path candidate = current.resolve("src" + File.separator + "test" + File.separator + "resources");
                    if (Files.isDirectory(candidate))
                    {
                        return candidate;
                    }
                    current = current.getParent();
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // 2. Explicit configured directory if present and existing on disk
        try
        {
            final String globalDir = Neodymium.aiConfiguration().getProperty("playbook.directory.global", null);
            if (globalDir != null && !globalDir.trim().isEmpty())
            {
                final Path configuredPath = Path.of(globalDir.trim());
                if (Files.isDirectory(configuredPath))
                {
                    return configuredPath;
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 3. Fallback to current working directory src/test/resources if it exists
        final Path localSrc = Path.of("src/test/resources");
        if (Files.isDirectory(localSrc))
        {
            return localSrc;
        }

        return null;
    }

    @Override
    public String resolveInclude(final String parentIdentifier, final String relativePath)
    {
        if (parentIdentifier == null || parentIdentifier.trim().isEmpty()
            || relativePath.startsWith("playbooks/") || relativePath.startsWith("ai-playbooks/") || relativePath.startsWith("src/") || relativePath.startsWith("/"))
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
