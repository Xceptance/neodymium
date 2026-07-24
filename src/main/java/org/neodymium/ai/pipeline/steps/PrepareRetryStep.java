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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete pipeline step executed before self-healing retries. Resets SUT state
 * by dismissing alerts, closing overlay modals/popups, and blurring focused inputs.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class PrepareRetryStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareRetryStep.class);

    /**
     * Constructs a PrepareRetryStep.
     */
    public PrepareRetryStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        // 1. Check if browser session is currently active
        if (WebDriverRunner.hasWebDriverStarted())
        {
            LOGGER.debug("Preparing SUT state for retry loop execution...");

            // 2. Safely dismiss active native alert dialogs if present
            try
            {
                WebDriverRunner.getWebDriver().switchTo().alert().dismiss();
                LOGGER.debug("   Dismissed active JavaScript alert dialog.");
            }
            catch (final Exception e)
            {
                // Expected when no alert modal is open on screen
            }

            // 3. Hide interfering UI overlays, cookie consent banners, and modal backdrop elements
            try
            {
                Selenide.executeJavaScript(
                    "document.querySelectorAll('.modal, .overlay, .popup, [role=\"dialog\"], .cookie-banner, #cookie-consent').forEach(el => el.style.display = 'none');"
                );
                LOGGER.debug("   Hidden blocking UI overlay elements via JS DOM execution.");
            }
            catch (final Exception e)
            {
                // Ignore JS execution issues when DOM is clean
            }

            // 4. Clear active element focus state from form input fields
            try
            {
                Selenide.executeJavaScript(
                    "if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) { document.activeElement.blur(); }"
                );
                LOGGER.debug("   Cleared input element focus state.");
            }
            catch (final Exception e)
            {
                // Ignore focus blur errors
            }
        }
    }
}
