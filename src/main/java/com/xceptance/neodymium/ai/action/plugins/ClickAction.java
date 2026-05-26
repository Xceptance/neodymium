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
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementShould;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ClickAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "CLICK"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public void preCheck(Action action, ActionExecutor executor) {
        try {
            SelenideElement element = executor.findElement(action);
            // Modern web frameworks tend to hide the actual input field and show a custom box.
            if ("input".equalsIgnoreCase(element.getTagName()) &&
                ("checkbox".equalsIgnoreCase(element.getAttribute("type")) || "radio".equalsIgnoreCase(element.getAttribute("type"))) && element.isDisplayed() == false) {
                element.should(com.codeborne.selenide.Condition.exist);
            } else {
                element.shouldBe(com.codeborne.selenide.Condition.visible);
            }
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not found or not visible for target '%s'", action.getTarget()), t);
        }
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "CLICK: click on an element (requires target)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        try {
            final SelenideElement element = executor.findElement(action);
            action.setElementContext(executor.extractElementContext(element));
            executor.scrollIntoView(element);
            
            if ("input".equalsIgnoreCase(element.getTagName()) &&
                ("checkbox".equalsIgnoreCase(element.getAttribute("type")) || "radio".equalsIgnoreCase(element.getAttribute("type"))) &&
                !element.isDisplayed()) {
                com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].click();", element);
            } else {
                element.click();
            }
        } catch (final org.openqa.selenium.ElementClickInterceptedException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Click intercepted on target '%s' (element: '%s')", action.getTarget(), action.getElementDetails()), e);
        } catch (final org.openqa.selenium.ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), e);
        } catch (final org.openqa.selenium.StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to execute action '%s'", action.getTarget()), t);
        }
    }
}
