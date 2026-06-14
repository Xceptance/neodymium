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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class RefreshAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshAction.class);

    @Override
    public String getActionName() { return "REFRESH"; }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        if ("REFRESH".equals(normalized))
        {
            return List.of(new Action("REFRESH", null, "Refresh page"));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "REFRESH: refresh the current page."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        LOG.debug("Refreshing page");
        Selenide.refresh();
    }
}
