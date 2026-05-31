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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.ai.playbook.Playbook;

/**
 * Verifies low-latency local Visual Playbook cache checks (dHash).
 * The first run establishes the visual baseline, and the second run
 * succeeds instantly offline by matching the cached baseline dHash.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
public final class StableVisualBaselineTest extends BaseAiTest
{
    @NeodymiumTest
    public void testStableVisualBaseline()
    {
        final int port = server.getPort();
        final String shopUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/index.html", port);
        open(shopUrl);

        // 1. Establish the baseline run (calls Gemini)
        Neodymium.ai().execute("Observe page visual consistency (glance)");

        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null)
        {
            playbook.setRecording(false);
            playbook.setCursor(0);
        }

        final long startTime = System.nanoTime();

        // 2. Immediate second run should hit the local dHash visual playbook cache
        Neodymium.ai().execute("Observe page visual consistency (glance)");

        final long durationUs = (System.nanoTime() - startTime) / 1000;
        
        // Assert that the cached check bypassed the LLM network and ran in under 1 second
        assertTrue(durationUs < 1000000, "Cached dHash baseline check took too long: " + durationUs + " us");
    }
}
