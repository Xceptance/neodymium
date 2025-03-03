package com.xceptance.neodymium.junit5.testclasses.urlfiltering;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;

@Browser("Chrome_1024x768")
public class IncludeOverExcludeTest extends AbstractNeodymiumTest
{
    @NeodymiumTest
    public void testIncludedURLisAllowed()
    {
        Selenide.open("https://www.google.com/");
    }

    @NeodymiumTest
    public void testNotIncludedURLisforbidden()
    {
        Selenide.open("https://www.xceptance.com/en/");
    }
}
