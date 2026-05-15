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

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class WaitAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(WaitAction.class);

    @Override
    public String getActionName() { return "WAIT"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "WAIT: wait for a specified duration or condition (requires value in ms)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        String target = action.getTarget();
        if (target != null && !target.isBlank()) {
            long timeoutMs = 10000; // ELEMENT_TIMEOUT.toMillis()
            if (action.getValue() != null && !action.getValue().isBlank()) {
                try {
                    timeoutMs = Long.parseLong(action.getValue());
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid timeout value for WAIT action: {}", action.getValue());
                }
            }
            LOG.debug("Waiting up to {} ms for element: {}", timeoutMs, target);
            executor.findElement(action).shouldBe(Condition.visible, Duration.ofMillis(timeoutMs));
        } else {
            int ms = 1000;
            if (action.getValue() != null && !action.getValue().isBlank()) {
                try {
                    ms = Integer.parseInt(action.getValue());
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid sleep value for WAIT action: {}", action.getValue());
                }
            }
            LOG.debug("Sleeping for {} ms", ms);
            Selenide.sleep(ms);
        }
    }
}
