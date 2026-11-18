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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for non-blocking atomic file writing, ensuring concurrent file readers
 * never encounter truncated or partially-written content during test execution.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AtomicFileUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileUtils.class);

    private AtomicFileUtils()
    {
    }

    /**
     * Writes the given string content to target path atomically via a temporary file and atomic replace.
     *
     * @param path target file path
     * @param content text content to write
     * @throws IOException if file creation or move fails
     */
    public static void writeStringAtomic(final Path path, final String content) throws IOException
    {
        if (path == null)
        {
            return;
        }

        final Path parent = path.getParent();
        final Path dir = parent != null ? parent : Paths.get(".");
        if (!Files.exists(dir))
        {
            Files.createDirectories(dir);
        }

        final String prefix = path.getFileName() != null ? path.getFileName().toString() + "_" : "atomic_";
        final Path tempFile = Files.createTempFile(dir, prefix, ".tmp");

        try
        {
            Files.writeString(tempFile, content != null ? content : "", StandardCharsets.UTF_8);

            try
            {
                Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (final Exception e)
            {
                LOGGER.debug("Atomic move failed for {}, falling back to StandardCopyOption.REPLACE_EXISTING: {}", path, e.getMessage());
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            if (Files.exists(tempFile))
            {
                try
                {
                    Files.delete(tempFile);
                }
                catch (final Exception ignored)
                {
                }
            }
        }
    }

    /**
     * Overload accepting a {@link File} object.
     *
     * @param file target file
     * @param content text content to write
     * @throws IOException if file operation fails
     */
    public static void writeStringAtomic(final File file, final String content) throws IOException
    {
        if (file == null)
        {
            return;
        }
        writeStringAtomic(file.toPath(), content);
    }
}
