package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.jupiter.api.Assertions;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;

@Browser("")
public class EmptyBrowser
{

    @NeodymiumTest
    public void testName() throws Exception
    {
        Assertions.fail();
    }
}
