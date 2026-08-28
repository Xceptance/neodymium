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

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to delegate read, write, and relative inclusion path resolution
 * for playbooks and recordings to abstract resources.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface PlaybookResourceManager
{
    /**
     * Opens an input stream to read a resource's raw content.
     *
     * @param identifier the resource path, database key, or classpath URI
     * @return the input stream to read the resource content
     * @throws IOException if an I/O error occurs
     */
    InputStream read(String identifier) throws IOException;

    /**
     * Writes raw content to a target identifier.
     *
     * @param identifier the resource path, database key, or classpath URI
     * @param content the content to write
     * @throws IOException if an I/O error occurs
     */
    void write(String identifier, String content) throws IOException;

    /**
     * Deletes the resource identified by the target identifier.
     *
     * @param identifier the resource path, database key, or classpath URI
     * @throws IOException if deletion fails
     */
    void delete(String identifier) throws IOException;

    /**
     * Resolves a relative resource identifier (like an include path)
     * against a parent playbook's identifier.
     *
     * @param parentIdentifier the identifier of the parent playbook
     * @param relativePath the relative target path (e.g. "includes/login.yaml")
     * @return the resolved absolute or fully qualified identifier
     */
    String resolveInclude(String parentIdentifier, String relativePath);
}
