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
 * Verifies that the Aura AST extractor and AI agent can target, interact with,
 * and audit elements nested across iframe boundaries without frame switching hacks.
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
public final class CrossFrameExtractionTest extends BaseAiTest
{
    @NeodymiumTest
    public void testCrossFrameAuraAstExtraction()
    {
        final int port = server.getPort();
        final String dashboardUrl = String.format("http://localhost:%d/AuraGlanceTest/dashboard/index.html", port);

        assertAiExecution(() ->
        {
            open(dashboardUrl);

            // Interact directly with components nested inside the dashboard iframe
            Neodymium.ai().execute("Click the 'Trigger Subpage Event' button inside the iframe. (hint: #dashboard-btn)");
            
            // Verify state update resolved correctly inside frame elements
            Neodymium.ai().execute("Verify that the frame state indicator text 'Internal Frame State Updated Successfully!' is visible inside the iframe.");
        });
    }
}
