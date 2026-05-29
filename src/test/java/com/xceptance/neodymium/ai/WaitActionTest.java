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

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the behavior of the WaitAction AI plugin.
 */
public final class WaitActionTest extends BaseAiTest
{
    /**
     * Verifies all three wait conditions: duration, wait-until-visible, and wait-until-hidden.
     */
    @NeodymiumTest
    public void testWaitHappyPath()
    {
        open(currentTestUrl);

        // Click start to trigger asynchronous loader transitions
        Neodymium.ai().execute("Click on the 'Start Process' button.");

        // 1. Wait for a specific duration
        Neodymium.ai().execute("Wait for 1 second.");

        // 2. Wait until loader element is hidden (disappears)
        Neodymium.ai().execute("Wait until 'Processing... Please wait...' is hidden.");

        // 3. Wait until success message is visible (appears)
        Neodymium.ai().execute("Wait until 'Process Completed Successfully!' is visible.");

        // Double check success state
        $("#success").shouldBe(Condition.visible);
    }
}
