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
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import java.io.File;
import java.io.IOException;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.tool.browser.BrowserToolProvider;

/**
 * Action plugin that uploads a file to an {@code <input type="file">} or styled dropzone container.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class UploadAction implements BrowserActionPlugin
{
    /**
     * Constructs a default UploadAction plugin.
     */
    public UploadAction()
    {
    }

    @Override
    public void execute(final Action action) throws IOException
    {
        if (action == null)
        {
            return;
        }

        if (!WebDriverRunner.hasWebDriverStarted())
        {
            throw new IOException("Cannot upload file: browser has not started yet.");
        }

        final String target = action.getTarget();
        final String value = action.getValue();
        final String filePath = (value != null && !value.isBlank()) ? value.trim() : null;

        if (filePath == null)
        {
            throw new IOException("Cannot upload file: action value (filePath) must not be empty.");
        }

        final File file = BrowserToolProvider.resolveUploadFile(filePath);
        final SelenideElement input = BrowserToolProvider.resolveFileInput(target);

        if (input == null || !input.exists())
        {
            throw new IOException("No <input type='file'> element found matching or within selector: " + target);
        }

        input.uploadFile(file);

        try
        {
            Selenide.executeJavaScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                    + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", input);
        }
        catch (final Exception ignored)
        {
        }
    }
}
