/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
 * // AI-generated: Gemini 3.1 Pro
 */
public enum ContextLevel
{
    /**
     * Minimal context with ZERO DOM elements. Used when an explicit inline (hint: #id) is provided.
     */
    HINT,

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
            case HINT -> LEAN;
            case LEAN -> STANDARD;
            case STANDARD -> VISUAL;
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
        return this == VISUAL;
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
