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

import java.util.Map;
import org.neodymium.ai.action.Action;
import com.codeborne.selenide.Selenide;

/**
 * Concrete action plugin executing NAVIGATE browser commands.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class NavigateAction implements BrowserActionPlugin
{
    /**
     * Constructs a NavigateAction.
     */
    public NavigateAction()
    {
    }

    /**
     * Opens the target URL.
     *
     * @param action the navigate action
     * @throws Exception if navigate fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null)
        {
            String url = action.getTarget();
            if ("url".equalsIgnoreCase(url) && action.getValue() != null)
            {
                url = action.getValue();
            }
            if (url != null)
            {
                Selenide.open(url);
            }
        }
    }
}
