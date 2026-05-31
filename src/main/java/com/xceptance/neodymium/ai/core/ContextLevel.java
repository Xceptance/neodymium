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
package com.xceptance.neodymium.ai.core;

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
     * clickable div/span, headings (h1-h5), and forms.
     * No text content blocks. No screenshot.
     * <p>
     * Sufficient for ~80% of instructions (click, type, select, navigate).
     */
    LEAN,

    /**
     * Everything in {@link #LEAN} plus all visible text content
     * ({@code p, span, li, td, div} elements with non-empty text).
     * No screenshot.
     * <p>
     * Equivalent to the previous {@code forValidation=true} mode.
     * Required for capture, verify, and assert instructions, or when
     * LEAN data is insufficient to disambiguate similar elements.
     */
    STANDARD,

    /**
     * Lean DOM (same as {@link #LEAN}) plus a page screenshot.
     * Used as the initial context when the instruction is explicitly tagged with `(visual)`.
     * Escalates directly to {@link #VISUAL} if needed.
     */
    VISUAL_LEAN,

    /**
     * Same DOM as {@link #STANDARD} plus a page screenshot sent as a
     * multimodal input. The LLM can visually identify elements that are
     * hidden from the DOM extractor (CSS pseudo-elements, SVG text,
     * canvas-rendered content) and map them back to the nearest
     * {@code data-neo-ref}.
     * <p>
     * This is the maximum available context level.
     */
    VISUAL;

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
            case HINT -> AXTREE;
            case AXTREE -> LEAN;
            case LEAN -> STANDARD;
            case STANDARD -> VISUAL;
            case VISUAL_LEAN -> VISUAL;
            case VISUAL -> null;
        };
    }

    /**
     * Whether this context level includes a screenshot alongside the DOM data.
     *
     * @return {@code true} if a screenshot is part of the context
     */
    public boolean includesScreenshot()
    {
        return this == VISUAL_LEAN || this == VISUAL;
    }

    /**
     * Whether this context level includes the full text content section
     * (paragraphs, spans, list items, table cells, divs with text).
     *
     * @return {@code true} if text content is included
     */
    public boolean includesTextContent()
    {
        return this == STANDARD || this == VISUAL;
    }
}
