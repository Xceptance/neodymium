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
 */
public enum LlmMode
{
    /** Standard AI agent mode for {@code @NeodymiumTest} test execution. */
    AGENT,

    /** Exploratory generator mode for {@code @NeodymiumTestGenerator} prompt generation. */
    GENERATOR
}
