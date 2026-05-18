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
 * // AI-generated: Gemini 3.1 Pro (High)
 */
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * Plugin action that captures text from an element and stores it as a variable
 * in the execution context for later use.
 */
public final class StoreAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(StoreAction.class);

    public static final String ACTION_NAME = "STORE";

    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    @Override
    public List<Action> parseDirectInstruction(final String instruction) {
        return null;
    }

    @Override
    public boolean requiresLlm(final Action action) {
        return false;
    }

    @Override
    public String getPromptInstructions() {
        return "STORE: Capture text from an element to use later. Requires \"target\" (the element to capture from) and \"value\" (the variable name to store the data under, e.g., 'subtotalPrice'). The captured text can be used in later actions as ${variableName}.";
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor) {
        final String variableName = action.getValue();
        if (variableName == null || variableName.isBlank()) {
            throw new ActionExecutionException("STORE action requires a 'value' indicating the variable name to store the captured text.");
        }

        final SelenideElement element = executor.findElement(action);
        final String text = element.getText();
        
        if (text != null) {
            final String trimmedText = text.trim();
            executor.setVariable(variableName, trimmedText);
            LOG.debug("   ✅ STORED variable '{}' with value '{}'", variableName, trimmedText);
        } else {
            throw new ActionExecutionException("STORE action failed: element text is null");
        }
    }
}
