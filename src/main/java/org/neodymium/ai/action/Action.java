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
package org.neodymium.ai.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single executable action parsed from LLM response or recording.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class Action
{
    private final String type;
    private final String target;
    private final List<String> value;
    private final String description;
    private final String reasoning;
    private final transient Map<String, Object> parameters = new HashMap<>();

    /**
     * Constructs a default empty action.
     */
    public Action()
    {
        this.type = "";
        this.target = "";
        this.value = new ArrayList<>();
        this.description = "";
        this.reasoning = "";
    }

    /**
     * Constructs an action with basic type, target, and description.
     *
     * @param type the action type (e.g. "CLICK")
     * @param target the target selector or URL
     * @param description the human-readable description
     */
    public Action(final String type, final String target, final String description)
    {
        this.type = type;
        this.target = target;
        this.value = new ArrayList<>();
        this.description = description;
        this.reasoning = "";
    }

    /**
     * Constructs a fully detailed action.
     *
     * @param type the action type (e.g. "TYPE")
     * @param target the target selector or URL
     * @param value the action values list
     * @param description the human-readable description
     * @param reasoning the reasoning of the action
     */
    public Action(final String type, final String target, final List<String> value, final String description, final String reasoning)
    {
        this.type = type;
        this.target = target;
        this.value = value != null ? new ArrayList<>(value) : new ArrayList<>();
        this.description = description;
        this.reasoning = reasoning;
    }

    /**
     * Returns the action type.
     *
     * @return the action type string
     */
    public final String getType()
    {
        return this.type;
    }

    /**
     * Returns the target selector or URL.
     *
     * @return the target selector or URL string
     */
    public final String getTarget()
    {
        return this.target;
    }

    /**
     * Returns the primary value of this action, or null if no values exist.
     *
     * @return the first value in the values list, or null
     */
    public final String getValue()
    {
        return (this.value != null && !this.value.isEmpty()) ? this.value.get(0) : null;
    }

    /**
     * Returns the complete values list of this action.
     *
     * @return the values list
     */
    public final List<String> getValues()
    {
        return this.value != null ? new ArrayList<>(this.value) : new ArrayList<>();
    }

    /**
     * Returns the human-readable description of this action.
     *
     * @return the description string
     */
    public final String getDescription()
    {
        return this.description;
    }

    /**
     * Returns the reasoning behind this action.
     *
     * @return the reasoning string
     */
    public final String getReasoning()
    {
        return this.reasoning;
    }

    /**
     * Returns the dynamic extensibility parameters map.
     *
     * @return the parameters map
     */
    public final Map<String, Object> getParameters()
    {
        return this.parameters;
    }
}
