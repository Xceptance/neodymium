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
 * Defines the operational mode of an {@link LlmClient}, which controls
 * which temperature setting is read from {@code AiConfiguration}.
 *
 * <ul>
 *   <li>{@link #AGENT} — used during {@code @NeodymiumTest} execution;
 *       reads {@code neodymium.ai.temperature} (low / deterministic).</li>
 *   <li>{@link #GENERATOR} — used during {@code @NeodymiumTestGenerator} exploration;
 *       reads {@code neodymium.ai.generate.temperature} (high / creative).</li>
 * </ul>
  *
 * // AI-generated: Gemini 2.0 Flash
*/
public enum LlmMode
{
    /** Standard AI agent mode for {@code @NeodymiumTest} test execution. */
    AGENT,

    /** Exploratory generator mode for {@code @NeodymiumTestGenerator} prompt generation. */
    GENERATOR
}
