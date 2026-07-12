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
package org.neodymium.ai.playbook;

import java.io.IOException;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.resources.PlaybookResourceManager;

/**
 * Interface representing a parser that loads execution playbooks from a resource
 * identifier using a delegated resource manager.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface PlaybookParser
{
    /**
     * Parses a playbook from a resource identifier using the provided manager.
     *
     * @param identifier the resource path, database key, or URI of the playbook
     * @param manager the resource manager to read file streams and resolve nested includes
     * @return the parsed immutable Playbook instance
     * @throws IOException if loading or parsing the playbook fails
     */
    Playbook parse(final String identifier, final PlaybookResourceManager manager) throws IOException;
}
