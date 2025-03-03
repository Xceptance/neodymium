package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.codeborne.selenide.Configuration;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultSelenideConfiguration
{
    @Test
    public void test1() throws Exception
    {
        Neodymium.timeout(1234);
        Assert.assertEquals(1234, Configuration.timeout);

        Neodymium.fastSetValue(true);
        Assert.assertEquals(true, Configuration.fastSetValue);

        Neodymium.clickViaJs(true);
        Assert.assertEquals(true, Configuration.clickViaJs);
    }

    @Test
    public void test2() throws Exception
    {
        Assert.assertEquals(3000, Configuration.timeout);
        Assert.assertEquals(false, Configuration.fastSetValue);
        Assert.assertEquals(false, Configuration.clickViaJs);
    }
}
