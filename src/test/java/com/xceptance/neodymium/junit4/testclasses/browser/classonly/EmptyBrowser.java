package com.xceptance.neodymium.junit4.testclasses.browser.classonly;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("")
public class EmptyBrowser
{

    @Test
    public void testName() throws Exception
    {
        Assert.fail();
    }
}
