package com.xceptance.neodymium.junit4.testclasses.urlfiltering;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ExcludeTest extends NeodymiumTest
{
    @Test
    public void testFirstURLisforbidden()
    {
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(100);
        Selenide.open("https://www.google.com/");
    }

    @Test
    public void testSecondURLisforbidden()
    {
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(100);
        Selenide.open("https://github.com");
    }
}
