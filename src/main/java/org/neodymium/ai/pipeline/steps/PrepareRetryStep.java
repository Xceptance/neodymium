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
package org.neodymium.ai.pipeline.steps;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Concrete pipeline step executed before self-healing retries. Resets SUT state
 * by dismissing alerts, closing overlay modals/popups, and blurring focused inputs.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PrepareRetryStep implements PipelineStep
{
    /**
     * Constructs a PrepareRetryStep.
     */
    public PrepareRetryStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            try
            {
                // 1. Safely dismiss any open JavaScript alert dialogues
                WebDriverRunner.getWebDriver().switchTo().alert().dismiss();
            }
            catch (final Exception e)
            {
                // Ignored (no alert dialogue present)
            }

            try
            {
                // 2. Hide common overlay modals/popups using Javascript
                Selenide.executeJavaScript(
                    "document.querySelectorAll('.modal, .overlay, .popup, [role=\"dialog\"], .cookie-banner, #cookie-consent').forEach(el => el.style.display = 'none');"
                );
            }
            catch (final Exception e)
            {
                // Ignored (Javascript execution failure or no matching elements)
            }

            try
            {
                // 3. Blur focused inputs to clear typing cursor/focus state
                Selenide.executeJavaScript(
                    "if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) { document.activeElement.blur(); }"
                );
            }
            catch (final Exception e)
            {
                // Ignored
            }
        }
    }
}
