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
package com.xceptance.neodymium.ai.action.plugins;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * AI action plugin for handling WAIT/sleep instructions.
 * This plugin parses natural language wait commands and executes them using Selenide.
 * It supports both waiting for a specific DOM element to become visible, or performing
 * an explicit pause/sleep in test execution.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public class WaitAction implements AiActionPlugin
{
    /**
     * Logger instance for debug and error statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(WaitAction.class);

    /**
     * Retrieves the identifier of the action plugin.
     * 
     * @return the name of the action, which is "WAIT"
     */
    @Override
    public String getActionName()
    {
        return "WAIT";
    }

    /**
     * Parses direct instructions that do not require full LLM analysis.
     * Looks for commands matching the pattern: WAIT <value>[s|ms].
     * 
     * @param instruction the input natural language instruction
     * @return a list containing a single parsed Action, or null if instruction doesn't match
     */
    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        // Normalize whitespaces and trim edges
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        
        // Compile pattern to match: e.g. "WAIT 5s" or "WAIT 500ms"
        final Pattern pattern = Pattern.compile("^WAIT\\s+(?<value>\\d+(?:\\.\\d+)?)(?<unit>s|ms)?$");
        final Matcher matcher = pattern.matcher(normalized);
        
        if (matcher.matches())
        {
            // Extract the raw duration value and unit from named groups
            final String valStr = matcher.group("value");
            final String unit = matcher.group("unit");
            final int ms;
            
            // Convert seconds to milliseconds if unit is 's'
            if (unit != null && unit.equalsIgnoreCase("s"))
            {
                final double seconds = Double.parseDouble(valStr);
                ms = (int) (seconds * 1000.0);
            }
            else
            {
                // Default to milliseconds if no unit or unit is 'ms'
                ms = (int) Double.parseDouble(valStr);
            }
            
            // Return the WAIT action with milliseconds value as string
            return List.of(new Action("WAIT", null, String.valueOf(ms), "Wait for " + ms + " ms"));
        }
        
        return null;
    }

    /**
     * Checks if this action requires live LLM calls.
     * 
     * @param action the action to evaluate
     * @return false, as wait/sleep execution is deterministic and handles itself without LLM
     */
    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    /**
     * Provides natural language guidelines to the LLM on how to construct a WAIT action.
     * 
     * @return the prompt description string
     */
    @Override
    public String getPromptInstructions()
    {
        return "WAIT: Wait for a target element to become available (set 'tg' to the locator. If a previous wait timed out, you MUST set 'v' to '20000' to extend the wait, otherwise leave 'v' empty or set it to a timeout in milliseconds) or sleep/pause execution (omit 'tg', set 'v' to the duration in milliseconds).";
    }

    /**
     * Executes the parsed WAIT action.
     * If a target selector is supplied, it waits for that element to become visible.
     * If no target is supplied, it pauses/sleeps the execution thread.
     * 
     * @param action       the parsed action containing targets and values
     * @param testInstance the context instance of the running test
     * @param executor     the action executor wrapper providing element lookup and interaction
     */
    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final String target = action.getTarget();
        
        // If target element selector is present, wait for element visibility
        if (target != null && !target.isBlank())
        {
            // Default timeout is 10 seconds
            long timeoutMs = 10000;
            
            // Check if a custom timeout was provided in the value field
            if (action.getValue() != null && !action.getValue().isBlank())
            {
                try
                {
                    timeoutMs = Long.parseLong(action.getValue());
                }
                catch (final NumberFormatException e)
                {
                    LOG.warn("Invalid timeout value for WAIT action: {}", action.getValue());
                }
            }
            
            LOG.debug("Waiting up to {} ms for element: {}", timeoutMs, target);
            
            // Wait until the element is visible in the page DOM
            executor.findElement(action).shouldBe(Condition.visible, Duration.ofMillis(timeoutMs));
        }
        else
        {
            // If no target is present, perform a thread sleep
            int ms = 1000;
            
            // Parse custom sleep duration from value if present
            if (action.getValue() != null && !action.getValue().isBlank())
            {
                try
                {
                    ms = Integer.parseInt(action.getValue());
                }
                catch (final NumberFormatException e)
                {
                    LOG.warn("Invalid sleep value for WAIT action: {}", action.getValue());
                }
            }
            
            LOG.debug("Sleeping for {} ms", ms);
            
            // Sleep the browser driver execution thread
            Selenide.sleep(ms);
        }
    }
}
