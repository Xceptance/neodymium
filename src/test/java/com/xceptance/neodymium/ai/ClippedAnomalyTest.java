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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies that the visual auditor can identify boundary container clipping and text truncations
 * as visual defects, throwing an AssertionError when the anomaly is present.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 */
@Browser("Chrome_1024x768")
public final class ClippedAnomalyTest extends BaseAiTest
{
    @NeodymiumTest
    public void testClippedLabelVisualAnomaly()
    {
        // Reset the active playbook and skip replay completely for negative tests
        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "true");
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "0");

        final int port = server.getPort();
        final String a11yUrl = String.format("http://localhost:%d/AuraGlanceTest/a11y/index.html", port);
        open(a11yUrl);

        // 1. Inject content boundary clippings
        Neodymium.ai().execute("Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)");
        
        try
        {
            Thread.sleep(500);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        Neodymium.ai().execute("Click the 'Inject Clipped Text' toggle. (hint: label[for='toggle-clipped'])");

        // 2. Expect immediate audit failure due to text truncation/clipped boundary cuts
        assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("Observe page visual consistency (glance). Assert that the description paragraph inside the 'Clipped Content & Text Boundaries' card is fully visible, and that NO words or lines of text are sliced horizontally or truncated at the bottom of the container. Do not click any toggle switches or perform any actions.");
        }, "Clipped container boundary cuts should throw an AssertionError");
    }
}
