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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.AuthenticationType;
import com.codeborne.selenide.BasicAuthCredentials;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Action plugin that parses and executes direct browser navigation steps, 
 * including optional support for basic HTTP authentication.
 * 
 * This plugin matches natural language instructions such as "open http://example.com"
 * or "visit http://example.com with basic auth username 'user' and password 'pass'"
 * and translates them into executable Selenium/Selenide actions.
 */
public final class NavigateAction implements AiActionPlugin
{
    /**
     * Logger instance for debug and tracing purposes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NavigateAction.class);

    /**
     * Returns the unique identifying name of this action plugin.
     *
     * @return the name "NAVIGATE"
     */
    @Override
    public String getActionName()
    {
        return "NAVIGATE";
    }

    /**
     * Parses the instruction statically to determine if it is a direct navigation command.
     * Uses regular expressions to match navigation phrases (e.g., "open", "visit", "go to")
     * followed by a URL and optional basic authentication parameters.
     *
     * @param instruction the natural language instruction to parse
     * @return a list containing the parsed navigation {@link Action}, or null if the instruction
     *         does not represent a direct navigation command
     */
    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        if (normalized.startsWith("OPEN ") || normalized.startsWith("NAVIGATE "))
        {
            final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "^(?:OPEN|NAVIGATE)\\s+(?<url>\\S+?)(?:\\s+(?i)with\\s+basic\\s+auth\\s+user\\s+\"(?<username>[^\"]*)\"\\s+password\\s+\"(?<password>[^\"]*)\")?$")
                .matcher(normalized);
            if (matcher.matches())
            {
                final String url = matcher.group("url");
                final String username = matcher.group("username");
                final String password = matcher.group("password");

                if (username != null && password != null)
                {
                    LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
                    return List.of(new Action("NAVIGATE", url, List.of(username, password),
                        "Navigate to " + url + " with basic auth (user: " + username + ", pass: " + password + ")"));
                }
                else if (Neodymium.configuration().basicAuthUsername() != null)
                {
                    final String configUser = Neodymium.configuration().basicAuthUsername();
                    final String configPass = Neodymium.configuration().basicAuthPassword();
                    LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
                    return List.of(new Action("NAVIGATE", url, List.of(configUser, configPass),
                        "Navigate to " + url + " with basic auth (user: " + configUser + ", pass: " + configPass + ")"));
                }
                else
                {
                    LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
                    return List.of(new Action("NAVIGATE", null, url, "Navigate to " + url));
                }
            }
        }
        return null;
    }

    /**
     * Returns whether execution of the parsed action requires remote LLM assistance.
     * Direct navigation actions are executed entirely locally.
     *
     * @param action the action to inspect
     * @return false
     */
    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    /**
     * Returns prompt formatting instructions for LLM prompt generation to teach the model
     * the syntax format of this NAVIGATE action.
     *
     * @return instruction syntax string
     */
    @Override
    public String getPromptInstructions()
    {
        return "NAVIGATE: Go to URL. Set v to the URL. For basic auth, set tg to the URL and v to ['user', 'pass'].";
    }

    /**
     * Executes the navigation action inside the active browser using Selenide.
     *
     * @param action       the action containing the target URL and optional credentials
     * @param testInstance the test class instance context
     * @param executor     the execution orchestrator
     * @throws ActionExecutionException if the navigation URL is missing or empty
     */
    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        String url = action.getValue();
        if (url == null || url.isBlank())
        {
            throw new ActionExecutionException("NAVIGATE action requires a 'value' (URL)");
        }
        
        // If target URL is set and we have at least two values (username and password),
        // execute navigation with HTTP basic authentication using Selenide's credentials API.
        if (StringUtils.isNotBlank(action.getTarget()) && action.getValues().size() > 1)
        {
            url = action.getTarget();
            final String username = action.getValues().get(0);
            final String password = action.getValues().get(1);
            LOG.debug("Navigating to: {} with basic auth {}/{}", url, username, password);
            Selenide.open(url, AuthenticationType.BASIC, new BasicAuthCredentials(username, password));
        }
        else
        {
            LOG.debug("Navigating to: {}", url);
            Selenide.open(url);
        }
        
        // Wait until the browser reports that the document load lifecycle state is fully complete.
        Selenide.Wait().until(d -> Selenide.executeJavaScript("return document.readyState").equals("complete"));
    }
}
