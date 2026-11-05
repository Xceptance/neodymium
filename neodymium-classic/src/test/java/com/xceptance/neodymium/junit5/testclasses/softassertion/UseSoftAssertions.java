package com.xceptance.neodymium.junit5.testclasses.softassertion;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.junit5.SoftAssertsExtension;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

@ExtendWith(SoftAssertsExtension.class)
@Browser("Chrome_headless")
public class UseSoftAssertions
{
    @NeodymiumTest
    public void validateSoftAssertion()
    {
        Neodymium.softAssertions(true);
        Selenide.open("https://blog.xceptance.com/");

        Assertions.assertEquals(Configuration.assertionMode, AssertionMode.SOFT);
        $("#notFound1").should(exist);
        $("#notFound2").should(exist);
        $("#masthead .search-toggle").click();
        $("#notFound3").should(exist);
        Assertions.assertThrows(ElementNotFound.class, () -> {
            $("#notFound4").click();
        });
    }
}
