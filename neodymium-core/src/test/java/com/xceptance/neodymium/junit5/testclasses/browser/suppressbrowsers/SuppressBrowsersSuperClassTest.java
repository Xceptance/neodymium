package com.xceptance.neodymium.junit5.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@SuppressBrowsers
public class SuppressBrowsersSuperClassTest
{
    @NeodymiumTest
    public void test(){
        assertNull(Neodymium.getDriver());
        assertNull(Neodymium.getWebDriverStateContainer());
    }
}
