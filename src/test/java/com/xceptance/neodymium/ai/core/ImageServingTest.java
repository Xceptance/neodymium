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
package com.xceptance.neodymium.ai.core;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import static com.codeborne.selenide.Selenide.open;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies that image MIME types served from the directory are correctly resolved
 * and rendered inside browser page components.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.OFFLINE_REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY,
    VerificationMode.HUD_LLM
})
public final class ImageServingTest extends BaseAiTest
{
    @NeodymiumTest
    public void testImageServingVerification()
    {
        final int port = server.getPort();
        final String shopUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/index.html", port);

        assertAiExecution(() ->
        {
            open(shopUrl);

            // Verify image visibility for the served poster artwork assets
            Neodymium.ai().execute("Verify that the 'Aura Neon Gradient Poster' card contains a visible image element.");
        });
    }
}
