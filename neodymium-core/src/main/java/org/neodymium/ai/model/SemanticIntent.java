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
 * Defines the primary operational or verification objective of a playbook instruction step,
 * classified just-in-time during the Pre-Execution Step Analysis Phase (PESAP).
 * <p>
 * Semantic intent categorization focuses downstream action extraction to relevant HTML element
 * types and enforces deterministic Java-level execution guards against mutating actions during assertions.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public enum SemanticIntent
{
    /**
     * Page and element verifications (text content, pattern matching, badges, messages, presence,
     * visibility, enabled/disabled state, checked/unchecked, focused state, counts, wait-for-text).
     */
    ASSERT,

    /**
     * Browser metadata verifications (page title, current URL, HTTP status) suitable for instant,
     * token-free fast-path assertions.
     */
    ASSERT_METADATA,

    /**
     * User clicking buttons, links, checkboxes, icons, tabs, or interactive triggers.
     */
    CLICK,

    /**
     * Form data entry into input fields, textareas, or contenteditable elements.
     */
    TYPE,

    /**
     * Selecting options from dropdowns, radio button groups, or list pickers.
     */
    SELECT,

    /**
     * Mouse hover, scrolling to element or viewport position, or revealing hidden hover menus.
     */
    HOVER_SCROLL,

    /**
     * Browser navigation operations (opening a URL, refresh, back, forward).
     */
    NAVIGATE,

    /**
     * Explicit temporal pauses, sleeps, or waiting for spinners and animation settling.
     */
    WAIT,

    /**
     * Extracting or reading values from the page into session variables.
     */
    STORE,

    /**
     * Conditional execution logic (If / Else execution branches).
     */
    BRANCH;

    /**
     * Checks whether this intent is an assertion category (state or metadata verification).
     *
     * @return true if this intent is {@link #ASSERT} or {@link #ASSERT_METADATA}, false otherwise
     */
    public boolean isAssertion()
    {
        return this == ASSERT || this == ASSERT_METADATA;
    }

    /**
     * Checks whether this intent represents an interactive DOM manipulation.
     *
     * @return true if this intent is {@link #CLICK}, {@link #TYPE}, {@link #SELECT}, or {@link #HOVER_SCROLL}
     */
    public boolean isInteraction()
    {
        return this == CLICK || this == TYPE || this == SELECT || this == HOVER_SCROLL;
    }

    /**
     * Checks whether this intent represents a mutating state change.
     *
     * @return true if this intent mutates the page or browser session
     */
    public boolean isMutating()
    {
        return this == CLICK || this == TYPE || this == SELECT || this == NAVIGATE;
    }

    /**
     * Safely parses a string name or code into a {@link SemanticIntent} enum value.
     *
     * @param code the intent name or code string
     * @return the resolved {@link SemanticIntent}, or null if parsing fails or input is null
     */
    public static SemanticIntent fromCode(final String code)
    {
        return fromString(code, null);
    }

    /**
     * Safely parses a string name or code into a {@link SemanticIntent} enum value with a fallback.
     *
     * @param name the intent name string
     * @param fallback the default fallback intent if parsing fails or input is null
     * @return the parsed {@link SemanticIntent} or fallback
     */
    public static SemanticIntent fromString(final String name, final SemanticIntent fallback)
    {
        if (name == null || name.trim().isEmpty())
        {
            return fallback;
        }
        final String normalized = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        try
        {
            return SemanticIntent.valueOf(normalized);
        }
        catch (final Exception ignored)
        {
            return switch (normalized)
            {
                case "ASSERTION", "VERIFY", "CHECK", "VALIDATE" -> ASSERT;
                case "METADATA", "ASSERT_URL", "ASSERT_TITLE", "URL", "TITLE" -> ASSERT_METADATA;
                case "INTERACTION", "BUTTON", "LINK", "PRESS" -> CLICK;
                case "INPUT", "ENTER", "FILL", "WRITE" -> TYPE;
                case "DROPDOWN", "CHOOSE", "OPTION", "RADIO" -> SELECT;
                case "HOVER", "SCROLL", "MOUSE_OVER" -> HOVER_SCROLL;
                case "OPEN", "GOTO", "BROWSE" -> NAVIGATE;
                case "PAUSE", "SLEEP", "DELAY" -> WAIT;
                case "EXTRACT", "SAVE", "READ", "SET" -> STORE;
                case "IF", "CONDITION", "IF_ELSE" -> BRANCH;
                default -> fallback;
            };
        }
    }
}
