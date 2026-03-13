package com.xceptance.neodymium.junit5.testclasses.multibrowser;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
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
        $(".navbar-brand a").shouldBe(visible);

        // Verifies the Navigation bar is visible
        $(".navbar-nav").shouldBe(visible);

        // Asserts there's categories in the nav bar.
        $$(".navbar-nav > li > a").shouldHave(sizeGreaterThan(0));

        // Asserts the first headline is there.
        $(".hero-title").shouldBe(matchText("[A-Z].{3,}"));
    }
}
