package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.Configuration;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class SelenideConfigurationShortcuts
{
    @NeodymiumTest
    public void validateSoftAssertionsion()
    {
        Assertions.assertEquals(Configuration.assertionMode, AssertionMode.STRICT);

        Neodymium.softAssertions(true);
        Assertions.assertEquals(Configuration.assertionMode, AssertionMode.SOFT);

        Neodymium.softAssertions(false);
        Assertions.assertEquals(Configuration.assertionMode, AssertionMode.STRICT);
    }

    @NeodymiumTest
    public void validateClickViaJs()
    {
        Assertions.assertEquals(Configuration.clickViaJs, false);

        Neodymium.clickViaJs(true);
        Assertions.assertEquals(Configuration.clickViaJs, true);

        Neodymium.clickViaJs(false);
        Assertions.assertEquals(Configuration.clickViaJs, false);
    }

    @NeodymiumTest
    public void validateFastSetValue()
    {
        Assertions.assertEquals(Configuration.fastSetValue, false);

        Neodymium.fastSetValue(true);
        Assertions.assertEquals(Configuration.fastSetValue, true);

        Neodymium.fastSetValue(false);
        Assertions.assertEquals(Configuration.fastSetValue, false);
    }

    @NeodymiumTest
    public void validateTimeout()
    {
        Assertions.assertEquals(Configuration.timeout, 3000);

        Neodymium.timeout(1000);
        Assertions.assertEquals(Configuration.timeout, 1000);

        Neodymium.timeout(2000);
        Assertions.assertEquals(Configuration.timeout, 2000);
    }
}
