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
 * Validates the behavior of the StoreAction AI plugin.
 */
public final class StoreActionTest extends BaseAiTest
{
    /**
     * Verifies extracting elements to dynamic variables, price adjustment, and interpolating them.
     */
    @NeodymiumTest
    public void testStoreHappyPath()
    {
        open(currentTestUrl);

        // Store standard element text
        Neodymium.ai().execute("Capture the order ID value and save it as variable 'myOrderId'.");

        // Store price amount with numeric adjustment
        Neodymium.ai().execute("Capture the price amount. Save it as variable 'myPrice' and store with adjustment.");

        // Type the stored dynamic variable into the input field
        Neodymium.ai().execute("Type the value of variable '${myOrderId}' into the verify input field.");

        // Click the verify button
        Neodymium.ai().execute("Click on the 'Verify' button.");

        // Assert that the verification succeeded
        $("#result").shouldHave(Condition.exactText("Verified successfully!"));
    }
}
