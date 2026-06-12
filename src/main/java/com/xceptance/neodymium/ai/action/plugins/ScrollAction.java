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
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ScrollAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "SCROLL"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "SCROLL: scroll to an element or position (requires tg)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        if (action != null && action.getTarget() != null && !action.getTarget().isBlank()) {
            final SelenideElement element = executor.findElement(action);
            executor.scrollIntoView(element);
        } else {
            // Scroll down by viewport height
            Selenide.executeJavaScript("window.scrollBy(0, window.innerHeight)");
        }
    }
}
