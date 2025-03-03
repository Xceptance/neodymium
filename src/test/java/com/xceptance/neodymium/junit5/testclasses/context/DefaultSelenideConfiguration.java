package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Configuration;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class DefaultSelenideConfiguration
{
    @NeodymiumTest
    public void test1() throws Exception
    {
        Neodymium.timeout(1234);
        Assertions.assertEquals(1234, Configuration.timeout);

        Neodymium.fastSetValue(true);
        Assertions.assertEquals(true, Configuration.fastSetValue);

        Neodymium.clickViaJs(true);
        Assertions.assertEquals(true, Configuration.clickViaJs);
    }

    @NeodymiumTest
    public void test2() throws Exception
    {
        Assertions.assertEquals(3000, Configuration.timeout);
        Assertions.assertEquals(false, Configuration.fastSetValue);
        Assertions.assertEquals(false, Configuration.clickViaJs);
    }
}
