package com.xceptance.neodymium.junit4.testclasses.popupblocker;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class PopupBlockerTestclass extends NeodymiumTest
{
    @Test
    public void testPopUpIsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup = "var e = document.createElement('div');"
                       + "e.innerHTML = 'testThing';"
                       + "e.setAttribute('id','myPopUp1');"
                       + "e.setAttribute('onclick','this.remove()');"
                       + "document.body.appendChild(e);";
        Selenide.executeJavaScript(popup, "");
        Selenide.sleep(1500);
        $("#myWindow").shouldNotBe(visible);
    }

    @Test
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

    @Test
    public void testPopUpWithSvgButtonIsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup = "var e = document.createElement('svg');"
                       + "e.innerHTML = 'testThing';"
                       + "e.setAttribute('id','myPopUp1');"
                       + "e.setAttribute('onclick','this.remove()');"
                       + "document.body.appendChild(e);";
        Selenide.executeJavaScript(popup, "");
        Selenide.sleep(1500);
        $("#myWindow").shouldNotBe(visible);
    }

    @Test
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

    @Test
    public void testMultiplePopUpsBlocked()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup1 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp1');\r\n"
                        + "e.setAttribute('onclick','this.remove()');\r\n"
                        + "document.body.appendChild(e);";
        String popup2 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp2');\r\n"
                        + "e.setAttribute('onclick','this.remove()');\r\n"
                        + "document.body.appendChild(e);";
        String popup3 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp3');\r\n"
                        + "e.setAttribute('onclick','this.remove()');\r\n"
                        + "document.body.appendChild(e);";
        Selenide.executeJavaScript(popup1, "");
        Selenide.executeJavaScript(popup3, "");
        Selenide.executeJavaScript(popup2, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);
        $("#myPopUp2").shouldNotBe(visible);
        $("#myPopUp3").shouldNotBe(visible);
    }

    @Test
    public void testPopUpsBlockedAfterAdditionalPageLoad()
    {
        Selenide.open("https://www.xceptance.com/");
        String popup1 = "var e = document.createElement(\"div\");\r\n"
                        + "e.innerHTML = \"testThing\";\r\n"
                        + "e.setAttribute('id','myPopUp1');\r\n"
                        + "e.setAttribute('onclick','this.remove()');\r\n"
                        + "document.body.appendChild(e);";
        Selenide.executeJavaScript(popup1, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);
        
        // Next page by load
        Selenide.open("https://blog.xceptance.com/");
        Selenide.executeJavaScript(popup1, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);
        
        // next page by click
        $$(".blogroll > li > a").findBy(exactText("XLT")).click();
        Selenide.executeJavaScript(popup1, "");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);

    }

    @Test
    public void testWithNoPopUp()
    {
        Selenide.open("https://www.xceptance.com/");
        Selenide.sleep(1500);
        $("#myPopUp1").shouldNotBe(visible);
    }
}
