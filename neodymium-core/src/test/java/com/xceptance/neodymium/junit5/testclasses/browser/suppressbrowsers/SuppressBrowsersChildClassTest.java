package com.xceptance.neodymium.junit5.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

public class SuppressBrowsersChildClassTest extends SuppressBrowsersSuperClassTest
{
    @NeodymiumTest
    public void test() {
        assertNull(Neodymium.getDriver());
        assertNull(Neodymium.getWebDriverStateContainer());
    }
}
