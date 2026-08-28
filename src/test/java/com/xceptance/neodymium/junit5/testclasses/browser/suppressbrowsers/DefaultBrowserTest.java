package com.xceptance.neodymium.junit5.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@Browser
public class DefaultBrowserTest
{
    @NeodymiumTest
    public void test() {
        assertNotNull(Neodymium.getDriver());
        assertNotNull(Neodymium.getWebDriverStateContainer());
        assertNotNull(Neodymium.getWebDriverStateContainer().getWebDriver());
    }
}
