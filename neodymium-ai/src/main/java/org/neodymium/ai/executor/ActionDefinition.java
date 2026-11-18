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
package org.neodymium.ai.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable record representing the structural metadata signature of a SUT action type.
 * Declares the expected parameters for validation.
 *
 * @param type the action type identifier (e.g., "CLICK", "TYPE", "NAVIGATE")
 * @param description Javadoc-style description of the action purpose and usage
 * @param parameterTypes map detailing expected parameter names and their Java class types
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record ActionDefinition(
    String type,
    String description,
    Map<String, Class<?>> parameterTypes
)
{
    /**
     * Canonical constructor that performs defensive copying of the parameterTypes map to guarantee immutability.
     */
    public ActionDefinition(
        final String type,
        final String description,
        final Map<String, Class<?>> parameterTypes
    )
    {
        this.type = type;
        this.description = description;
        this.parameterTypes = parameterTypes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(parameterTypes));
    }
}
