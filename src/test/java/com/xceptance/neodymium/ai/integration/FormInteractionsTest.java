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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies interactive forms entry, client-side input validations,
 * reactive pricing calculations, and synchronous overlap visual assertions.
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
public final class FormInteractionsTest extends BaseAiTest
{
    @NeodymiumTest
    public void testFormInteractionsAndVerifications()
    {
        final int port = server.getPort();
        final String formsUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/forms.html", port);

        assertAiExecution(() ->
        {
            open(formsUrl);

            // 1. Fill fields and trigger validation warnings
            Neodymium.ai().execute("Type 'Jane Smith' into the 'Full Name' field.");
            Neodymium.ai().execute("Type 'invalidemail' into the 'Email Address' field.");
            Neodymium.ai().execute("Click the 'Create Account' button.");
            Neodymium.ai().execute("Verify that the email format invalid warning 'This email format is invalid.' is visible.");

            // 2. Verify reactive order summaries
            Neodymium.ai().execute("Type '2' into the 'Neon Gradient Poster' quantity field. (hint: #qty-poster-1)");
            Neodymium.ai().execute("Verify that the Total Price is '$39.98'.");
        });

        // 3. Inject visual overlap defect and assert immediate visual audit failure
        Neodymium.ai().execute("Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)");
        
        try
        {
            Thread.sleep(500);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        Neodymium.ai().execute("Click the 'Inject Element Overlap' toggle. (hint: label[for='toggle-overlap'])");
        
        // Disable retries for this step as we expect it to fail immediately
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "0");

        // The page close/cancel button now overlaps the heading; an immediate visual audit should throw
        assertThrows(AssertionError.class, () -> 
        {
            Neodymium.ai().execute("Observe page visual consistency (glance). Assert that the 'Cancel' button does not overlap with the 'Security Password' field or adjacent form elements.");
        }, "Visual overlap of cancel button over heading should throw an AssertionError");
    }
}
