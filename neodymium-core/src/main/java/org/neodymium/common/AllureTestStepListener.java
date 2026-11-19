/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common;

import com.codeborne.selenide.Configuration;
import org.neodymium.util.Neodymium;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

import static org.neodymium.common.TestStepListener.URL_CHANGED_STEP_MESSAGE;
import static org.neodymium.util.AllureAddons.currentStepHasChildren;
import static io.qameta.allure.model.Status.PASSED;

public class AllureTestStepListener implements StepLifecycleListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureTestStepListener.class);

    private static boolean driverNullWarningLogged = false;

    @Override
    public void beforeStepStop(StepResult result)
    {
        // if the driver is not set, we can't take screenshots
        if (Neodymium.getDriver() == null)
        {
            if (!driverNullWarningLogged)
            {
                LOGGER.warn(
                    "Driver is not set in Neodymium and Selenide driver is used. Skipping screenshot capture for each test step. To enable screenshots per"
                        + " step, add the @Browser annotation to your test class or method.");
                driverNullWarningLogged = true;
            }
            return;
        }

        /*
        All possible cases
        0 0 - fail !screenshotPerStep   -> screenshot
        0 1 - fail screenshotPerStep    -> screenshot
        1 0 - pass !screenshotPerStep   -> no screenshot
        1 1 - pass screenshotPerStep    -> screenshot
         */

        // don't take a screenshot for passed steps if screenshotPerStep is false
        if (result.getStatus() == PASSED && !Neodymium.configuration().screenshotPerStep())
        {
            return;
        }

        // only take a screenshot in the most inner step
        if (currentStepHasChildren() && !Neodymium.configuration().screenshotPerStep())
        {
            return;
        }

        // if the result is stil passed screenshotPerStep is true and if the current step is a Selenide step we don't want to take a screenshot
        if (result.getStatus() == PASSED)
        {
            String name = result.getName();

            // safety check for null name
            if (name == null)
            {
                return;
            }

            // skip screenshots for steps with names starting with $(, $$(, $x(, or $$x( or for URL changed steps
            // but don't skip steps that open a URL, to ensure that we capture the initial page load
            if (name.startsWith(URL_CHANGED_STEP_MESSAGE)
                || (name.startsWith("$(") && !name.startsWith("$(\"open\") "))
                || name.startsWith("$$(")
                || name.startsWith("$x(")
                || name.startsWith("$$x("))
            {
                return;
            }
        }

        try
        {
            ScreenshotWriter.doScreenshot("beforeStepStop_screenshot_" + System.currentTimeMillis() + "_" + new Random().nextLong(),
                                          (result.getStatus() != PASSED) && Configuration.config().screenshots());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
