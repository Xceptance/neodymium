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

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PlaybookResourceManager} path resolution behaviors.
 * Validates the core interface contract and logic using an anonymous mock resolver.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookResourceManagerTest
{
    /**
     * Verifies relative include path resolution against a parent identifier path.
     * Uses a mock implementation of PlaybookResourceManager that resolves relative paths
     * and double-dot parent directory shifts.
     */
    @Test
    public void testMockPathResolution()
    {
        // Anonymous mock implementation to test basic interface signature and path resolution rules
        final PlaybookResourceManager manager = new PlaybookResourceManager()
        {
            @Override
            public java.io.InputStream read(final String identifier)
            {
                return null;
            }

            @Override
            public void write(final String identifier, final String content)
            {
            }

            @Override
            public void delete(final String identifier)
            {
            }

            @Override
            public String resolveInclude(final String parentIdentifier, final String relativePath)
            {
                if (parentIdentifier == null || relativePath == null)
                {
                    return relativePath;
                }
                
                final int lastSlash = parentIdentifier.lastIndexOf('/');
                if (lastSlash == -1)
                {
                    return relativePath;
                }
                
                final String parentDir = parentIdentifier.substring(0, lastSlash);
                
                // Relative path resolution logic for testing parent directory shifts ("../")
                if (relativePath.startsWith("../"))
                {
                    final int secondLastSlash = parentDir.lastIndexOf('/');
                    if (secondLastSlash == -1)
                    {
                        return relativePath.substring(3);
                    }
                    return parentDir.substring(0, secondLastSlash) + "/" + relativePath.substring(3);
                }
                
                return parentDir + "/" + relativePath;
            }
        };

        // Assert simple nested relative folder resolution
        assertEquals("src/test/resources/common/login.yaml", 
            manager.resolveInclude("src/test/resources/my-playbook.yaml", "common/login.yaml"));

        // Assert double-dot folder resolution back to parent dir
        assertEquals("playbooks/login.yaml", 
            manager.resolveInclude("playbooks/checkout/checkout.yaml", "../login.yaml"));
    }
}
