/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai;

import static com.codeborne.selenide.Selenide.open;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies silent step skipping behaviors when dynamic page components (such as promo banners)
 * are conditionally hidden, utilizing the case-insensitive '(optional)' tag.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
public final class OptionalBannerTest extends BaseAiTest
{
    @NeodymiumTest
    public void testOptionalBannerSilentBypass()
    {
        final int port = server.getPort();
        final String dashboardUrl = String.format("http://localhost:%d/AuraGlanceTest/dashboard/index.html", port);

        assertTwoPhaseExecution(() ->
        {
            open(dashboardUrl);

            // 1. Hide the conditional banner using control console
            Neodymium.ai().execute("Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)");
            
            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            Neodymium.ai().execute("Click the 'Show Promo Banner' toggle to hide it. (hint: label[for='toggle-banner'])");

            // 2. Attempting to close the hidden banner with (optional) should bypass silently without failures
            Neodymium.ai().execute("Click 'Close Banner' (optional)");
        });
    }
}
