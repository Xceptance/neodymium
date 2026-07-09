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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.layer.ElementCondition;
import com.xceptance.neodymium.util.layer.FoundElement;

/**
 * Plugin action that asserts the presence, visibility, or specific content of a given element,
 * or validates the current URL of the page.
 */
public final class AssertAction implements AiActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(AssertAction.class);

    /**
     * The unique identifier name for this action type.
     */
    public static final String ACTION_NAME = "ASSERT";

    /**
     * Gets the unique name of this action plugin.
     *
     * @return the action name
     */
    @Override
    public String getActionName()
    {
        return ACTION_NAME;
    }

    /**
     * Parses a direct string instruction (not supported by this plugin).
     *
     * @param instruction the raw instruction text
     * @return a list of Actions or null if not supported
     */
    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        return null;
    }

    /**
     * Checks if this action requires LLM processing to execute.
     *
     * @param action the action to evaluate
     * @return true if LLM is needed, false otherwise
     */
    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    /**
     * Gets the prompt instructions describing how the AI should format this action.
     *
     * @return the prompt instruction string
     */
    @Override
    public String getPromptInstructions()
    {
        return Neodymium.interaction().prompts().getAssertPromptInstructions();
    }


    /**
     * Executes the assertion action. Validates either the current page URL or the state of a specific DOM element.
     *
     * @param action       the AI-generated action definition
     * @param testInstance the test class instance (not directly used)
     * @param executor     the underlying executor instance providing element resolution
     */
    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final String expected = action.getValue();

        // Handle URL assertions (matching target names like "url", "currentUrl", or "pageUrl")
        if ("url".equalsIgnoreCase(action.getTarget()) || "currentUrl".equalsIgnoreCase(action.getTarget()) || "pageUrl".equalsIgnoreCase(action.getTarget()))
        {
            if (expected == null)
            {
                throw new ActionExecutionException("URL assertion requires a 'value' (the expected URL)");
            }

            // For silent checks, retrieve the URL directly and verify contains criteria without active waiting
            if (action.isSilent())
            {
                final String actualUrl = Neodymium.interaction().getCurrentUrl();
                if (actualUrl == null || !actualUrl.contains(expected))
                {
                    throw new ActionExecutionException("Silent condition not met: URL does not contain '" + expected + "'");
                }
                LOG.debug("   ✅ Silent URL Assertion passed for: '{}'", expected);
                return;
            }

            // Wait until the current page URL updates and matches/contains the expected string
            final long deadline = System.currentTimeMillis() + Neodymium.interaction().getTimeout();
            boolean matched = false;
            while (System.currentTimeMillis() < deadline)
            {
                final String current = Neodymium.interaction().getCurrentUrl();
                if (current != null && current.contains(expected))
                {
                    matched = true;
                    break;
                }
                try { Thread.sleep(200); } catch (final InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            if (!matched)
            {
                final String actualUrl = Neodymium.interaction().getCurrentUrl();
                Neodymium.interaction().wrapAssertionError(() ->
                {
                    throw new AssertionError(String.format("Assertion failed: Expected URL to contain '%s' but was '%s'", expected, actualUrl));
                });
            }
            LOG.debug("   ✅ URL Assertion passed for: '{}'", expected);
            return;
        }

        // Handle Element assertions
        final boolean isAbsenceCheck = "hidden".equalsIgnoreCase(expected) || "[hidden]".equalsIgnoreCase(expected)
                || "absent".equalsIgnoreCase(expected) || "[absent]".equalsIgnoreCase(expected);

        final FoundElement element;
        if (isAbsenceCheck)
        {
            FoundElement tempElement = null;
            try
            {
                tempElement = executor.findElement(action);
            }
            catch (final Exception e)
            {
                LOG.debug("   ✅ Assertion passed (element does not exist/absent): '{}'", expected);
                return;
            }
            element = tempElement;
        }
        else
        {
            element = executor.findElement(action);
        }

        // If no value is specified, assert that the target element simply exists on the page
        if (expected == null)
        {
            if (action.isSilent())
            {
                if (!element.matchesCondition(ElementCondition.exist()))
                {
                    throw new ActionExecutionException("Silent condition not met: Element does not exist");
                }
            }
            else
            {
                element.assertCondition(ElementCondition.exist());
            }
            LOG.debug("   ✅ Element exists: {}", action);
            return;
        }

        try
        {
            // Focus assertion: Verify that the targeted element is currently focused (document.activeElement)
            if ("focused".equals(expected))
            {
                if (action.isSilent())
                {
                    if (!element.matchesCondition(ElementCondition.focused()))
                    {
                        throw new ActionExecutionException("Silent condition not met: Element not focused");
                    }
                }
                else
                {
                    element.assertCondition(ElementCondition.focused());
                }
            }
            // Visibility assertion: Verify that the targeted element is visible on the page
            else if ("visible".equals(expected))
            {
                if (action.isSilent())
                {
                    if (!element.matchesCondition(ElementCondition.visible()))
                    {
                        throw new ActionExecutionException("Silent condition not met: Element not visible");
                    }
                }
                else
                {
                    element.assertCondition(ElementCondition.visible());
                }
            }
            // Hidden/absence assertion: Verify that the targeted element is hidden or does not exist
            else if ("hidden".equalsIgnoreCase(expected) || "[hidden]".equalsIgnoreCase(expected)
                    || "absent".equalsIgnoreCase(expected) || "[absent]".equalsIgnoreCase(expected))
            {
                if (action.isSilent())
                {
                    if (element.matchesCondition(ElementCondition.visible()))
                    {
                        throw new ActionExecutionException("Silent condition not met: Element is visible");
                    }
                }
                else
                {
                    element.assertCondition(ElementCondition.hidden());
                }
            }
            // Content assertions: verify text, regex matching, or attribute contents of the target element
            else
            {
                final ElementCondition cond;
                if (isRegexPattern(expected))
                {
                    cond = ElementCondition.matchesRegex(expected);
                }
                else
                {
                    // Check if value is format attrName=attrValue (e.g. class="active" or placeholder="search")
                    final Matcher attributeMatcher = Pattern.compile("^([a-zA-Z0-9_-]+)=[\"']?(.*?)[\"']?$").matcher(expected);
                    if (attributeMatcher.matches())
                    {
                        final String attrName = attributeMatcher.group(1);
                        final String attrValue = attributeMatcher.group(2);
                        // Match by specific attribute, falling back to text/value/any-attr
                        // Use a compound OR by checking multiple conditions
                        cond = ElementCondition.attrIs(attrName, attrValue);
                    }
                    else
                    {
                        // Composite: text contains OR value is OR any attr contains
                        // Use TEXT_CONTAINS as primary — assertCondition backend will also match via textContent
                        cond = ElementCondition.textContains(expected);
                    }
                }

                if (action.isSilent())
                {
                    // For silent checks, also try anyAttrContains and value as fallbacks
                    final boolean matches = element.matchesCondition(cond)
                            || element.matchesCondition(ElementCondition.anyAttrContains(expected))
                            || element.matchesCondition(ElementCondition.valueIs(expected));
                    if (!matches)
                    {
                        throw new ActionExecutionException("Silent condition not met: Element does not match '" + expected + "'");
                    }
                }
                else
                {
                    element.assertCondition(cond);
                }
            }
            LOG.debug("   ✅ Assertion passed for: '{}'", expected);
        }
        catch (final ActionExecutionException e)
        {
            throw e;
        }
        catch (final Throwable e)
        {
            String attributesStr = "Error retrieving attributes";
            try
            {
                final Map<String, String> attributes = element.getAllAttributes();
                attributesStr = attributes != null ? attributes.toString() : "{}";
            }
            catch (final Exception ex)
            {
                // Ignore attribute retrieval errors
            }
            final String actualDetails = String.format("Text: '%s', Value: '%s', Attributes: %s",
                    element.getText(), element.getAttribute("value"), attributesStr);
            Neodymium.interaction().wrapAssertionError(() ->
            {
                throw new AssertionError(String.format("Assertion failed: '%s' not found in common or element attributes. Found: [%s]", expected, actualDetails), e);
            });
        }
    }

    /**
     * Determines whether the given string is likely intended as a regular expression pattern
     * rather than a literal value.
     *
     * @param str the string to evaluate
     * @return true if the string appears to be a regex pattern, false otherwise
     */
    private boolean isRegexPattern(final String str)
    {
        if (str == null)
        {
            return false;
        }
        return str.contains("\\") || str.contains("[") || str.contains("]") || str.contains("{") || str.contains("}")
                || str.contains(".*") || str.contains(".+") || str.contains("|") || str.startsWith("^") || str.endsWith("$");
    }
}
