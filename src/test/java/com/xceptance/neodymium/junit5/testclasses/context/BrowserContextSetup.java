package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class BrowserContextSetup
{
    @BeforeAll
    public static void beforeClass()
    {
        Assertions.assertNull(Neodymium.getBrowserProfileName());
        Assertions.assertNull(Neodymium.getBrowserName());
    }

    @NeodymiumTest
    public void test1() throws Exception
    {
        Assertions.assertEquals("Chrome_headless", Neodymium.getBrowserProfileName());
        Assertions.assertEquals("chrome", Neodymium.getBrowserName());
    }

    @AfterAll
    public static void afterClass()
    {
        Assertions.assertNull(Neodymium.getBrowserProfileName());
        Assertions.assertNull(Neodymium.getBrowserName());
    }
}
