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
 * Verifies that the visual auditor can identify asymmetrical margin misalignments and layout shifts
 * as visual defects, throwing an AssertionError when the anomaly is present.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 */
@Browser("Chrome_1024x768")
public final class LayoutShiftAnomalyTest extends BaseAiTest
{
    @NeodymiumTest
    public void testLayoutShiftVisualAnomaly()
    {
        // Reset the active playbook and skip replay completely for negative tests
        Neodymium.setAiPlaybook(null);
        Neodymium.getData().put("skipReplay", "true");
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "0");

        final int port = server.getPort();
        final String a11yUrl = String.format("http://localhost:%d/AuraGlanceTest/a11y/index.html", port);
        open(a11yUrl);

        // 1. Inject asymmetrical layout misalignment
        Neodymium.ai().execute("Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)");
        
        try
        {
            Thread.sleep(500);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        Neodymium.ai().execute("Click the 'Inject Layout Misalignment' toggle. (hint: label[for='toggle-shift'])");

        // 2. Expect immediate audit failure due to grid rows misalignment shift
        assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("Observe page visual consistency (glance). Assert that all cards in the main layout, including the 'Grid Alignment & Shifts' card, are perfectly aligned in the grid and that the 'Grid Alignment & Shifts' card is NOT tilted, rotated, offset, or shifted.");
        }, "Grid layout column misalignment shifts should throw an AssertionError");
    }
}
