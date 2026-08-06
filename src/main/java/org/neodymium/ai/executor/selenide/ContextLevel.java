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
package org.neodymium.ai.executor.selenide;

/**
 * Defines the amount of page context sent to the LLM for a given instruction step.
 * The agent always starts at {@link #LEAN} and escalates to richer levels only
 * when the LLM explicitly requests it (via an {@code ESCALATE} response status)
 * or when an action execution fails.
 * <p>
 * Escalation does <em>not</em> count against the retry budget — it is a
 * different strategy, not a repeated attempt with the same data.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public enum ContextLevel
{
    /**
     * Minimal context with ZERO DOM elements. Used when an explicit inline (hint: #id) is provided.
     */
    HINT,

    /**
     * Compact browser-native accessibility tree. Provides structural and semantic outline
     * of the interactive DOM elements for ultra-low token consumption.
     */
    AXTREE,

    /**
     * Interactive elements only: links, buttons, inputs, selects, textareas,
     * clickable div/span, headings (h1-h6), and forms.
     * Excludes long paragraph copy (&lt;p&gt;) and non-interactive static text blocks. No screenshot.
     */
    LEAN,

    /**
     * Everything in {@link #LEAN} plus standard static text content
     * ({@code p, span, li, td, div} elements with non-empty text). No screenshot.
     */
    STANDARD,

    /**
     * Enhanced textual context including all DOM text nodes, full data-* &amp; aria-* attributes,
     * un-truncated URLs, complete data tables, and 5-level deep parent text context. No screenshot.
     */
    RICH,

    /**
     * Minimal page header (URL and title only, zero DOM element nodes) plus a page screenshot.
     * Used for pure visual checks and assertions where no element interaction is required.
     */
    VISUAL,

    /**
     * LEAN DOM (same as {@link #LEAN}) plus a page screenshot.
     * Used when visual element interaction is required.
     */
    VISUAL_LEAN,

    /**
     * RICH DOM (same as {@link #RICH}) plus a page screenshot sent as a multimodal input.
     * Maximum available context level.
     */
    VISUAL_RICH;

    /**
     * Returns the next escalation level, or {@code null} if already at the
     * maximum level.
     *
     * @return the next higher context level, or {@code null}
     */
    public ContextLevel escalate()
    {
        return switch (this)
        {
            case HINT -> LEAN;
            case AXTREE -> STANDARD;
            case LEAN -> STANDARD;
            case STANDARD -> RICH;
            case RICH -> VISUAL;
            case VISUAL -> VISUAL_LEAN;
            case VISUAL_LEAN -> VISUAL_RICH;
            case VISUAL_RICH -> null;
        };
    }

    /**
     * Whether this context level includes a screenshot alongside the DOM data.
     *
     * @return {@code true} if a screenshot is part of the context
     */
    public boolean includesScreenshot()
    {
        return this == VISUAL || this == VISUAL_LEAN || this == VISUAL_RICH;
    }

    /**
     * Whether this context level includes standard static text content
     * (paragraphs, spans, list items, table cells, divs with text).
     *
     * @return {@code true} if text content is included
     */
    public boolean includesTextContent()
    {
        return this == STANDARD || this == RICH || this == VISUAL_RICH;
    }

    /**
     * Whether this context level includes rich data attributes, aria metadata,
     * and deep parent text context.
     *
     * @return {@code true} if rich metadata is included
     */
    public boolean includesRichMetadata()
    {
        return this == RICH || this == VISUAL_RICH;
    }

    /**
     * Safely parses a string name into a ContextLevel enum value.
     *
     * @param name the level name string
     * @param fallback the default fallback level if parsing fails or input is null
     * @return the parsed ContextLevel or fallback
     */
    public static ContextLevel fromString(final String name, final ContextLevel fallback)
    {
        if (name == null || name.trim().isEmpty())
        {
            return fallback;
        }
        try
        {
            return ContextLevel.valueOf(name.trim().toUpperCase());
        }
        catch (final Exception ignored)
        {
            return fallback;
        }
    }
}
