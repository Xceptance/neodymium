/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit5.testclasses.multibrowser;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.junit.jupiter.api.Assertions;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Browser("Safari_Browserstack")
public class BrowserstackHomePageTest
{
    @NeodymiumTest
    public void testVisitingHomepage()
    {
        // Goto the home page
        Selenide.open("https://www.xceptance.com/en/");

        // verify the opened browser is safari browser via navigator object, which contains information about the
        // browser
        Assertions.assertTrue(() -> Selenide.executeJavaScript("return navigator.userAgent.indexOf(\"Safari\")>-1;"));

        // short validation to check that the correct page was opened, should be moved to OpenHomePageFlow

        // basic validation
        // Verifies the company Logo and name are visible.
        $("a.navbar-brand").shouldBe(visible);

        // Verifies the Navigation bar is visible
        $(".navbar-nav").shouldBe(visible);

        // Asserts there's categories in the nav bar.
        $$(".navbar-nav > li > a").shouldHave(sizeGreaterThan(0));

        // Asserts the first headline is there.
        $(".hero-title").shouldBe(matchText("[A-Z].{3,}"));
    }
}
