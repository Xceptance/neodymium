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
package com.xceptance.neodymium.junit5.testclasses.popupblocker;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;

@Browser("Chrome_1024x768")
public class PopupBlockerTestclass extends AbstractNeodymiumTest
{
    @NeodymiumTest
    public void testPopUpIsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup = "var e = document.createElement('div');"
                       + "e.innerHTML = 'testThing';"
                       + "e.setAttribute('id','myPopUp1');"
                       + "document.body.appendChild(e);"
                       + "e.addEventListener('click', function() {\n"
                       + "        this.remove();\n"
                       + "});";
        Selenide.executeJavaScript(popup, "");
        Selenide.sleep(1500);
        $("#myWindow").shouldNotBe(visible);
    }

    @NeodymiumTest
    public void testPopUpWithQuotesSelectorIsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup = "var e = document.createElement('div');"
                       + "e.innerHTML = 'testThing';"
                       + "e.setAttribute('data-testid','closeIcon');"
                       + "document.body.appendChild(e);"
                       + "e.addEventListener('click', function() {\n"
                       + "        this.remove();\n"
                       + "});";
        Selenide.executeJavaScript(popup);
        Selenide.sleep(1500);
        $("[data-testid='closeIcon']").shouldNotBe(visible);
    }

    @NeodymiumTest
    public void testPopUpWithSvgButtonIsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup = "var e = document.createElement('svg');"
                       + "e.innerHTML = 'testThing';"
                       + "e.setAttribute('id','myPopUp1');"
                       + "document.body.appendChild(e);"
                       + "e.addEventListener('click', function() {\n"
                       + "        this.remove();\n"
                       + "});";
        Selenide.executeJavaScript(popup, "");
        Selenide.sleep(1500);
        $("#myWindow").shouldNotBe(visible);
    }

    @NeodymiumTest
    public void testPopUpIsNotBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup = "var e = document.createElement(\"div\");\r\n"
                       + "e.innerHTML = \"testThing\";\r\n"
                       + "e.setAttribute('id','anotherWindow');\r\n"
                       + "document.body.appendChild(e);"
                       + "e.addEventListener('click', function() {\n"
                       + "        this.remove();\n"
                       + "});";
        Selenide.executeJavaScript(popup, "");
        Selenide.sleep(1500);
        $("#anotherWindow").shouldBe(visible);
    }

    @NeodymiumTest
    public void testMultiplePopUpsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup1 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp1');\r\n"
                        + "document.body.appendChild(e);"
                        + "e.addEventListener('click', function() {\n"
                        + "        this.remove();\n"
                        + "});";
        String popup2 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp2');\r\n"
                        + "e.setAttribute('onclick','this.remove()');\r\n"
                        + "document.body.appendChild(e);"
                        + "e.addEventListener('click', function() {\n"
                        + "        this.remove();\n"
                        + "});";
        String popup3 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp3');\r\n"
                        + "document.body.appendChild(e);"
                        + "e.addEventListener('click', function() {\n"
                        + "        this.remove();\n"
                        + "});";
        String popup4 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp4');\r\n"
                        + "document.body.appendChild(e);"
                        + "e.addEventListener('click', function() {\n"
                        + "        this.remove();\n"
                        + "});";
        Selenide.executeJavaScript(popup1, "");
        Selenide.executeJavaScript(popup2, "");
        Selenide.executeJavaScript(popup3, "");
        Selenide.executeJavaScript(popup4, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);
        $("#myPopUp2").shouldNotBe(visible);
        $("#myPopUp3").shouldNotBe(visible);
        $("#myPopUp4").shouldBe(visible);
    }

    @NeodymiumTest
    public void testPopUpsBlockedAfterAdditionalPageLoad()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup1 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp1');\r\n"
                        + "document.body.appendChild(e);"
                        + "e.addEventListener('click', function() {\n"
                        + "        this.remove();\n"
                        + "});";
        Selenide.executeJavaScript(popup1, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);

        // Next page by load
        Selenide.open("https://blog.xceptance.com/");
        Selenide.executeJavaScript(popup1, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);

        // next page by click
        $$("a").findBy(exactText("XLT")).click();
        Selenide.executeJavaScript(popup1, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);

    }

    @NeodymiumTest
    public void testWithNoPopUp()
    {
        Selenide.open("https://www.xceptance.com/");
        Selenide.sleep(1500);
        $("#myWindow").shouldNotBe(visible);
    }
}
