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

/**
 * Thrown when a variable placeholder (e.g., '${varName}') in an instruction or template
 * cannot be resolved from session data.
 *
 * Extends {@link IllegalArgumentException} to maintain backward compatibility while
 * allowing callers to specifically catch and filter variable resolution failures.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class UnresolvableVariableException extends IllegalArgumentException
{
    private static final long serialVersionUID = 1L;

    private final String variableName;

    private final String template;

    /**
     * Constructs an UnresolvableVariableException with the missing variable name and original template.
     *
     * @param variableName the name of the missing variable placeholder (without '${' and '}')
     * @param template the original template string where resolution failed
     */
    public UnresolvableVariableException(final String variableName, final String template)
    {
        super("Unresolvable variable placeholder '${" + variableName + "}' in template: \"" + template + "\"");
        this.variableName = variableName;
        this.template = template;
    }

    /**
     * Gets the name of the unresolvable variable placeholder.
     *
     * @return the variable name
     */
    public String getVariableName()
    {
        return this.variableName;
    }

    /**
     * Gets the original template containing the unresolvable placeholder.
     *
     * @return the template string
     */
    public String getTemplate()
    {
        return this.template;
    }
}
