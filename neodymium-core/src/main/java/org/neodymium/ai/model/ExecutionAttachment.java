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
package org.neodymium.ai.model;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing an artifact attachment associated with a test execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionAttachment implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String path;
    private final String type;
    private final String contentType;
    private final boolean openByDefault;
    private final File sourceFile;
    private final String textContent;

    /**
     * Constructs an ExecutionAttachment referencing an existing file on disk.
     *
     * @param name display name of the attachment
     * @param path relative path or filename
     * @param type logical type (e.g. video, yaml, log, image, file)
     * @param contentType MIME type (e.g. video/mp4, text/plain)
     * @param openByDefault true if UI should display expanded by default
     * @param sourceFile source file reference, or null
     * @param textContent in-memory text content, or null
     */
    public ExecutionAttachment(
        final String name,
        final String path,
        final String type,
        final String contentType,
        final boolean openByDefault,
        final File sourceFile,
        final String textContent)
    {
        this.name = Objects.requireNonNull(name, "Attachment name must not be null");
        final String rawPath = (path != null && !path.isBlank()) ? path : name;
        this.path = sanitizeAttachmentPath(rawPath);
        this.type = type != null ? type : inferTypeFromName(name);
        this.contentType = contentType != null ? contentType : inferContentType(name);
        this.openByDefault = openByDefault;
        this.sourceFile = sourceFile;
        this.textContent = textContent;
    }

    /**
     * Sanitizes a raw attachment path or filename to a clean, safe single-segment filename.
     *
     * @param raw the raw path or filename
     * @return sanitized safe filename
     */
    public static String sanitizeAttachmentPath(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return "attachment";
        }
        String p = raw.trim();

        // 1. If absolute path (Unix / Windows), take only the filename
        if (p.startsWith("/") || p.startsWith("\\") || p.matches("^[a-zA-Z]:.*"))
        {
            final int lastSep = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
            if (lastSep >= 0 && lastSep < p.length() - 1)
            {
                p = p.substring(lastSep + 1).trim();
            }
        }

        // 2. Strip relative traversal prefixes
        while (p.startsWith("../") || p.startsWith("..\\"))
        {
            p = p.substring(3).trim();
        }

        // 3. Relative subpaths: replace slashes with underscore
        p = p.replace('/', '_').replace('\\', '_');

        // 4. Remove path traversal dots, URI query / hash, illegal Windows characters and control characters
        p = p.replace("..", "").replace('?', '_').replace('#', '_');
        p = p.replaceAll("[:*?\"<>|]", "_");
        p = p.replaceAll("[\\p{Cntrl}]", "");

        // Strip leading underscores resulting from traversal separator stripping unless raw was hidden file
        while (p.startsWith("_") && !raw.trim().startsWith(".") && !raw.trim().startsWith("_"))
        {
            p = p.substring(1);
        }

        // 5. If starting with dot (hidden file in Unix), replace leading dot with underscore
        while (p.startsWith("."))
        {
            p = "_" + p.substring(1);
        }

        p = p.trim();
        if (p.isEmpty() || "_".equals(p))
        {
            return "attachment";
        }

        // 5. Cap filename stem while preserving extension
        final int extIdx = p.lastIndexOf('.');
        final String ext = (extIdx > 0 && extIdx > p.length() - 12) ? p.substring(extIdx) : "";
        final String stem = ext.isEmpty() ? p : p.substring(0, extIdx);
        final String cappedStem = stem.length() > 80 ? stem.substring(0, 80) : stem;

        return cappedStem + ext;
    }

    public static ExecutionAttachment ofFile(final String name, final File file, final boolean openByDefault)
    {
        Objects.requireNonNull(file, "Source file must not be null");
        final String effectiveName = (name != null && !name.isBlank()) ? name : file.getName();
        return new ExecutionAttachment(
            effectiveName,
            effectiveName,
            inferTypeFromName(effectiveName),
            inferContentType(effectiveName),
            openByDefault,
            file,
            null);
    }

    public static ExecutionAttachment ofText(
        final String name,
        final String textContent,
        final String contentType,
        final boolean openByDefault)
    {
        Objects.requireNonNull(name, "Attachment name must not be null");
        return new ExecutionAttachment(
            name,
            name,
            inferTypeFromName(name),
            contentType != null ? contentType : inferContentType(name),
            openByDefault,
            null,
            textContent);
    }

    public String getName()
    {
        return this.name;
    }

    public String getPath()
    {
        return this.path;
    }

    public String getType()
    {
        return this.type;
    }

    public String getContentType()
    {
        return this.contentType;
    }

    public boolean isOpenByDefault()
    {
        return this.openByDefault;
    }

    public File getSourceFile()
    {
        return this.sourceFile;
    }

    public String getTextContent()
    {
        return this.textContent;
    }

    private static String inferTypeFromName(final String filename)
    {
        if (filename == null)
        {
            return "file";
        }
        final String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov"))
        {
            return "video";
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml"))
        {
            return "yaml";
        }
        if (lower.endsWith(".log") || lower.endsWith(".txt"))
        {
            return "log";
        }
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif"))
        {
            return "image";
        }
        if (lower.endsWith(".json"))
        {
            return "json";
        }
        return "file";
    }

    private static String inferContentType(final String filename)
    {
        if (filename == null)
        {
            return "application/octet-stream";
        }
        final String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4"))
        {
            return "video/mp4";
        }
        if (lower.endsWith(".webm"))
        {
            return "video/webm";
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml"))
        {
            return "text/yaml";
        }
        if (lower.endsWith(".log") || lower.endsWith(".txt"))
        {
            return "text/plain";
        }
        if (lower.endsWith(".png"))
        {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif"))
        {
            return "image/gif";
        }
        if (lower.endsWith(".json"))
        {
            return "application/json";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final ExecutionAttachment that = (ExecutionAttachment) o;
        return this.openByDefault == that.openByDefault && Objects.equals(this.name, that.name) && Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.name, this.path, this.openByDefault);
    }

    @Override
    public String toString()
    {
        return "ExecutionAttachment{" +
            "name='" + this.name + '\'' +
            ", path='" + this.path + '\'' +
            ", type='" + this.type + '\'' +
            ", openByDefault=" + this.openByDefault +
            '}';
    }
}
