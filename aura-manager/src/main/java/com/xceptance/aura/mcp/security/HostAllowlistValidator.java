/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.mcp.security;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Validates file paths and network targets against SSRF and path traversal attacks.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class HostAllowlistValidator
{
    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "169.254.169.254", // Cloud metadata IP
        "metadata.google.internal",
        "instance-data"
    );

    /**
     * Verifies that the relative test file path does not escape the project boundary.
     *
     * @param testPath the requested relative test path
     * @return true if safe and contained within allowed directory
     */
    public boolean isSafeTestPath(final String testPath)
    {
        if (testPath == null || testPath.isBlank())
        {
            return false;
        }

        final Path normalized = Paths.get(testPath).normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..") || testPath.contains("../") || testPath.contains("..\\"))
        {
            return false;
        }

        final String pathStr = normalized.toString().replace('\\', '/').toLowerCase();
        return pathStr.endsWith(".yaml") || pathStr.endsWith(".yml");
    }

    /**
     * Verifies that target URL does not point to sensitive metadata or restricted local services.
     *
     * @param urlString target URL string
     * @return true if target host is allowed
     */
    public boolean isSafeTargetUrl(final String urlString)
    {
        if (urlString == null || urlString.isBlank())
        {
            return false;
        }

        try
        {
            final URI uri = URI.create(urlString);
            final String host = uri.getHost();
            if (host == null || host.isBlank())
            {
                return false;
            }

            final String lowerHost = host.toLowerCase();
            return !BLOCKED_HOSTS.contains(lowerHost);
        }
        catch (final Exception e)
        {
            return false;
        }
    }
}
