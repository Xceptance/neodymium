package com.xceptance.neodymium.junit4.testclasses.softassertion;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.junit.SoftAsserts;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class UseSoftAssertions
{
    @Rule
    public SoftAsserts softAsserts = new SoftAsserts();

    @Test
    public void validateSoftAssertion()
    {
        Neodymium.softAssertions(true);
        Selenide.open("https://blog.xceptance.com/");

        Assert.assertEquals(Configuration.assertionMode, AssertionMode.SOFT);
        $("#notFound1").should(exist);
        $("#notFound2").should(exist);
        $("#masthead .search-toggle").click();
        $("#notFound3").should(exist);
        Assert.assertThrows(ElementNotFound.class, () -> {
            $("#notFound4").click();
        });
    }
}
