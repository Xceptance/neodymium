package com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser
public class DefaultBrowserTest
{
    @Test
    public void test() {
        assertNotNull(Neodymium.getDriver());
        assertNotNull(Neodymium.getWebDriverStateContainer());
        assertNotNull(Neodymium.getWebDriverStateContainer().getWebDriver());
    }
}
