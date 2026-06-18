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
package com.xceptance.neodymium.ai.integration;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import static com.codeborne.selenide.Selenide.open;


import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies visual audits on a modern React-like SPA, testing asynchronous loading
 * states (suspense loader skeletons), partial AJAX DOM re-renders, and HTMX content swaps.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY,
    VerificationMode.HUD_LLM
})
/**
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ReactSpaTest extends BaseAiTest
{

    @NeodymiumTest
    public void testAsynchronousReactSpaAudits()
    {
        final int port = server.getPort();
        final String spaUrl = String.format("http://localhost:%d/AuraGlanceTest/spa/index.html", port);

        assertAiExecution(() ->
        {
            open(spaUrl);

            // 1. Initial page load displays reactive loading suspense skeletons.
            // The Selenide automated AI waiting handles the asynchronity and waits for the loaders to clear.
            Neodymium.ai().execute("Verify that the active sessions count '1,842' is visible on the page.");

            // 2. Click the Live Feeds tab to trigger an asynchronous HTMX/AJAX partial page swap
            Neodymium.ai().execute("Click 'Live Feeds (AJAX)'.");

            try
            {
                Thread.sleep(1500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            // Wait for AJAX load completion (displays loading spinner before swapping table)
            Neodymium.ai().execute("Verify that the live feeds data table displays the HTMX log 'Asynchronous partial HTML page swap executed successfully.'.");
        });
    }
}
