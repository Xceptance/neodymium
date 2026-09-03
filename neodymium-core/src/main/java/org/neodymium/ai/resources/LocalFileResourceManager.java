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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Concrete implementation of {@link PlaybookResourceManager} that reads,
 * writes, and resolves relative files on the local filesystem.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LocalFileResourceManager implements PlaybookResourceManager
{
    /**
     * The root base directory under which all relative file operations are resolved.
     */
    private final Path baseDirectory;

    /**
     * Constructs a LocalFileResourceManager with the specified base directory.
     *
     * @param baseDirectory the base directory for file operations
     */
    public LocalFileResourceManager(final Path baseDirectory)
    {
        this.baseDirectory = baseDirectory;
    }

    /**
     * Opens an input stream to read a resource's raw content from the local disk.
     *
     * @param identifier the relative resource path
     * @return the input stream to read the file
     * @throws IOException if the file does not exist or cannot be read
     */
    @Override
    public InputStream read(final String identifier) throws IOException
    {
        final Path target = this.baseDirectory.resolve(identifier).normalize();
        if (!Files.exists(target))
        {
            throw new FileNotFoundException("File not found: " + target);
        }
        return Files.newInputStream(target);
    }

    /**
     * Writes raw content to a file on the local disk atomically using a temporary file.
     * Automatically creates any missing parent directories.
     *
     * @param identifier the relative resource path
     * @param content the content to write
     * @throws IOException if writing to the file fails
     */
    @Override
    public void write(final String identifier, final String content) throws IOException
    {
        final Path target = this.baseDirectory.resolve(identifier).normalize();
        final Path parent = target.getParent();
        final Path dir = parent != null ? parent : this.baseDirectory;
        Files.createDirectories(dir);

        final Path tempFile = Files.createTempFile(dir, "recording-", ".tmp");
        try
        {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try
            {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (final AtomicMoveNotSupportedException e)
            {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public void delete(final String identifier) throws IOException
    {
        final Path target = this.baseDirectory.resolve(identifier).normalize();
        Files.deleteIfExists(target);
    }

    /**
     * Resolves a relative resource inclusion path against a parent file path.
     * Normalizes the result and returns a string with standardized forward slashes.
     *
     * @param parentIdentifier the identifier of the parent file
     * @param relativePath the relative path to resolve
     * @return the normalized relative path string
     */
    @Override
    public String resolveInclude(final String parentIdentifier, final String relativePath)
    {
        if (parentIdentifier == null || parentIdentifier.isEmpty()
            || relativePath.startsWith("playbooks/") || relativePath.startsWith("ai-playbooks/") || relativePath.startsWith("src/") || relativePath.startsWith("/"))
        {
            return Path.of(relativePath).normalize().toString().replace('\\', '/');
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
