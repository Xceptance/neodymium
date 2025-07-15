package com.xceptance.neodymium.junit4.testclasses.multibrowser;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@RunWith(NeodymiumRunner.class)
@Browser("Safari_Browserstack")
public class BrowserstackHomePageTest
{
    @Test
    public void testVisitingHomepage()
    {
        // Goto the home page
        Selenide.open("https://www.xceptance.com/en/");

        // verify the opened browser is safari browser via navigator object, which contains information about the
        // browser
        Assert.assertTrue(Selenide.executeJavaScript("return navigator.userAgent.indexOf(\"Safari\")>-1;"));

        // short validation to check that the correct page was opened, should be moved to OpenHomePageFlow

        // basic validation
        // Verifies the company Logo and name are visible.
        $("#navigation .navbar-brand a").shouldBe(visible);

        // Verifies the Navigation bar is visible
        $("#navigation ul.nav").shouldBe(visible);

        // Asserts there's categories in the nav bar.
        $$("#navigation ul.nav > li > a").shouldHave(sizeGreaterThan(0));

        // Asserts the first headline is there.
        $("#main h1").shouldBe(matchText("[A-Z].{3,}"));

        // Verifies the "services" section is there.
        // Asserts there's at least 1 item in the list.
        $$("#main .row.strip a").shouldHave(sizeGreaterThan(0));

        // Verifies the company button is there.
        $$("p.lead > a").shouldHave(sizeGreaterThan(0));
    }
}
