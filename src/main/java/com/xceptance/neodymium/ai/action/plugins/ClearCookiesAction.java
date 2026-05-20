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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ClearCookiesAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(ClearCookiesAction.class);

    @Override
    public String getActionName() { return "CLEAR_COOKIES"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String patternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.clearCookies", "(?i)^(?:clear\\s+cookies|reset\\s+session|clear\\s+all\\s+cookies)$");
        if (java.util.regex.Pattern.compile(patternStr).matcher(instruction.strip()).find()) {
            return List.of(new Action("CLEAR_COOKIES", null, "Clear cookies"));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "CLEAR_COOKIES: clear browser cookies and local storage."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        LOG.debug("Clearing cookies");
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
    }
}
