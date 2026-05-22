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
  *
 * // AI-generated: Gemini 2.0 Flash
*/
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.AuthenticationType;
import com.codeborne.selenide.BasicAuthCredentials;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.Neodymium;

public class NavigateAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(NavigateAction.class);

    @Override
    public String getActionName() { return "NAVIGATE"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String authPatternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.urlWithBasicAuth", "(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?:\\/\\/\\S+?)(?=[.,!?;]?(?:\\s|$))(?:\\s+.*?\\b(?:with|using)?\\s*basic\\s+auth\\s+(?:username|user)\\s+['\"](?<username>.*?)['\"]\\s+(?:and\\s+)?(?:password|pass)\\s+['\"](?<password>.*?)['\"]).*$");
        java.util.regex.Matcher authMatcher = java.util.regex.Pattern.compile(authPatternStr).matcher(instruction.strip());
        String urlPatternStr = com.xceptance.neodymium.util.Neodymium.configuration()
            .getProperty("neodymium.ai.agent.pattern.url",
                "(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?:\\/\\/\\S+?)(?=[.,!?;]?(?:\\s|$))(\\.)*$");
        java.util.regex.Matcher urlMatcher = java.util.regex.Pattern.compile(urlPatternStr).matcher(instruction.strip());

        // let's check if our prompt needs this or if we have it inside our config
        if (authMatcher.find() || (urlMatcher.find() && Neodymium.configuration().basicAuthUsername() != null)) {
            if (authMatcher.find() ) {
                final String url = authMatcher.group(1);
                final String username = authMatcher.group("username");
                final String password = authMatcher.group("password");
                if (username != null && password != null)
                {
                    LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
                    return List.of(new Action("NAVIGATE", url, List.of(username, password), "Navigate to " + url + " with basic auth (user: " + username
                                                                                            + ", pass: " + password + ")"));
                }
            }
            else
            {
                final String url = urlMatcher.group(1);
                final String username = Neodymium.configuration().basicAuthUsername();
                final String password = Neodymium.configuration().basicAuthPassword();
                if (username != null && password != null)
                {
                    LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
                    return List.of(new Action("NAVIGATE", url, List.of(username, password), "Navigate to " + url + " with basic auth (user: " + username
                                                                                            + ", pass: " + password + ")"));
                }
            }
        }

        if (urlMatcher.find())
        {
            final String url = urlMatcher.group(1);
            LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
            return List.of(new Action("NAVIGATE", null, url, "Navigate to " + url));
        }

        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() {
        return "NAVIGATE: navigate to a specified URL (target must be empty, value must be the URL). If basic auth is needed, set target to the URL, and value to a JSON array with username and password, e.g. [\"user\", \"pass\"].";
    }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        String url = action.getValue();
        if (url == null || url.isBlank()) {
            throw new ActionExecutor.ActionExecutionException("NAVIGATE action requires a 'value' (URL)");
        }
        if (StringUtils.isNotBlank(action.getTarget()) && action.getValues().size() > 1) {
            url = action.getTarget();
            String username = action.getValues().get(0);
            String password = action.getValues().get(1);
            LOG.debug("Navigating to: {} with basic auth {}/{}", url, username, password);
            Selenide.open(url, AuthenticationType.BASIC, new BasicAuthCredentials(username, password));
        } else {
            LOG.debug("Navigating to: {}", url);
            Selenide.open(url);
        }
        Selenide.Wait().until(d -> Selenide.executeJavaScript("return document.readyState").equals("complete"));
    }
}
