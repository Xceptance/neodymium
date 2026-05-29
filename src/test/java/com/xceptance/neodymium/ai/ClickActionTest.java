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

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the behavior of the ClickAction AI plugin.
 */
public final class ClickActionTest extends BaseAiTest
{
    @NeodymiumTest
    public void testClickStandardButton()
    {
        open(currentTestUrl);

        Neodymium.ai().execute("Click the Submit Order button.");

        // Verify the action worked by checking the result div
        $("#result").shouldHave(Condition.exactText("Order Submitted!"));
    }

    @NeodymiumTest
    public void testClickHiddenElement()
    {
        open(currentTestUrl);

        // Expect an exception because the element is hidden and cannot be clicked
        Assertions.assertThrows(AssertionError.class, () -> {
            Neodymium.ai().execute("Click the Secret Admin Button.");
        });
    }

    @NeodymiumTest
    public void testClickDisabledButton()
    {
        open(currentTestUrl);

        // Expect an exception because the button is disabled
        Assertions.assertThrows(AssertionError.class, () -> {
            Neodymium.ai().execute("Click the Disabled Order Button.");
        });
    }

    @NeodymiumTest
    public void testClickInvisibleCheckbox()
    {
        open(currentTestUrl);

        // The checkbox itself is invisible, but ClickAction should fall back to a JavaScript click and succeed
        Neodymium.ai().execute("Click the Accept Terms checkbox.");

        $("#result").shouldHave(Condition.exactText("Checked"));
    }

    @NeodymiumTest
    public void testClickAnchorLink()
    {
        open(currentTestUrl);

        Neodymium.ai().execute("Click on the Go to Target Destination link.");

        $("#result").shouldHave(Condition.exactText("Link Clicked!"));
    }

    @NeodymiumTest
    public void testClickSvgIcon()
    {
        open(currentTestUrl);

        Neodymium.ai().execute("Click on the Add Item button.");

        $("#result").shouldHave(Condition.exactText("SVG Button Clicked!"));
    }

    @NeodymiumTest
    public void testClickTableRow()
    {
        open(currentTestUrl);

        Neodymium.ai().execute("Click on the row for customer Bob Jones.");

        $("#result").shouldHave(Condition.exactText("Selected #1002"));
    }
}
