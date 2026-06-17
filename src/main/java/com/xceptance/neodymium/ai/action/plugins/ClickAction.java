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
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementShould;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

import com.xceptance.neodymium.ai.action.SelectorParser;

public class ClickAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "CLICK"; }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        if (normalized.startsWith("CLICK "))
        {
            final String target = normalized.substring(6).trim();
            if (target.isEmpty())
            {
                throw new IllegalArgumentException("Selector target for CLICK command cannot be empty");
            }
            final SelectorParser.ParsedSelector parsed = SelectorParser.parse(target);
            return List.of(new Action("CLICK", parsed.getExpression(), "Click " + parsed.getExpression()));
        }
        return null;
    }

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
    public String getPromptInstructions() { return "CLICK: click on an element (requires tg)."; }

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
