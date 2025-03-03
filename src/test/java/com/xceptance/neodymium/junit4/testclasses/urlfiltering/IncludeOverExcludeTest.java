package com.xceptance.neodymium.junit4.testclasses.urlfiltering;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class IncludeOverExcludeTest extends NeodymiumTest
{
    @Test
    public void testIncludedURLisAllowed()
    {
        Selenide.open("https://www.google.com/");
    }

    @Test
    public void testNotIncludedURLisforbidden()
    {
        Selenide.open("https://www.xceptance.com/en/");
    }
}
