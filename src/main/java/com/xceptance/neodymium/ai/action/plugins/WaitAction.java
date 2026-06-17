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
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "^WAIT\\s+(?<value>\\d+(?:\\.\\d+)?)(?<unit>s|ms)?$").matcher(normalized);
        if (matcher.matches())
        {
            final String valStr = matcher.group("value");
            final String unit = matcher.group("unit");
            final int ms;
            if (unit != null && unit.equalsIgnoreCase("s"))
            {
                final double seconds = Double.parseDouble(valStr);
                ms = (int) (seconds * 1000.0);
            }
            else
            {
                ms = (int) Double.parseDouble(valStr);
            }
            return List.of(new Action("WAIT", null, String.valueOf(ms), "Wait for " + ms + " ms"));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "WAIT: wait for a specified duration or condition (requires v in ms)."; }

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
