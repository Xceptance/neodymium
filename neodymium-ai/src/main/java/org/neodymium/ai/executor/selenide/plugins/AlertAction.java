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
package org.neodymium.ai.executor.selenide.plugins;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import java.io.IOException;
import org.neodymium.ai.action.Action;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action plugin that resolves native browser modal dialogs (alert, confirm, prompt).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class AlertAction implements BrowserActionPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertAction.class);

    /**
     * Constructs a default AlertAction plugin.
     */
    public AlertAction()
    {
    }

    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        if (!WebDriverRunner.hasWebDriverStarted())
        {
            throw new IOException("Cannot handle alert: browser has not started yet.");
        }

        final Alert alert;
        try
        {
            alert = Selenide.switchTo().alert();
        }
        catch (final NoAlertPresentException e)
        {
            throw new IOException("No native browser alert or dialog is present.", e);
        }

        final String value = action.getValue() != null ? action.getValue().trim() : "";
        final String target = action.getTarget() != null ? action.getTarget().trim() : "";

        // Check if prompt text is supplied
        final String promptText;
        if (!target.isBlank() && !"ALERT".equalsIgnoreCase(target) && !"CONFIRM".equalsIgnoreCase(target) && !"PROMPT".equalsIgnoreCase(target))
        {
            promptText = target;
        }
        else if (!value.equalsIgnoreCase("ACCEPT") && !value.equalsIgnoreCase("DISMISS") && !value.isBlank())
        {
            promptText = value;
        }
        else
        {
            promptText = null;
        }

        if (promptText != null)
        {
            LOGGER.debug("Sending prompt text to native dialog: {}", promptText);
            alert.sendKeys(promptText);
        }

        if ("DISMISS".equalsIgnoreCase(value) || "CANCEL".equalsIgnoreCase(value))
        {
            LOGGER.debug("Dismissing native browser dialog");
            alert.dismiss();
        }
        else
        {
            LOGGER.debug("Accepting native browser dialog");
            alert.accept();
        }
    }
}
