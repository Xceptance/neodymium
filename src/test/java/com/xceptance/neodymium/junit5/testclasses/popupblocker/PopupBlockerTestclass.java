package com.xceptance.neodymium.junit5.testclasses.popupblocker;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
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
                       + "e.setAttribute('id','myWindow');"
                       + "e.setAttribute('onclick','this.remove()');"
                       + "document.body.appendChild(e);";
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
                       + "e.setAttribute('onclick','this.remove()');"
                       + "document.body.appendChild(e);";
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
                       + "e.setAttribute('id','myWindow');"
                       + "e.setAttribute('onclick','this.remove()');"
                       + "document.body.appendChild(e);";
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
                       + "e.setAttribute('onclick','this.remove()');\r\n"
                       + "document.body.appendChild(e);";
        Selenide.executeJavaScript(popup, "");
        Selenide.sleep(1500);
        $("#anotherWindow").shouldBe(visible);
    }

    @NeodymiumTest
    public void testWithNoPopUp()
    {
        Selenide.open("https://www.xceptance.com/");
        Selenide.sleep(1500);
        $("#myWindow").shouldNotBe(visible);
    }
}
