package com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class DefaultBrowserTest
{
    @Test
    public void test() {
        assertNotNull(Neodymium.getDriver());
        assertNotNull(Neodymium.getWebDriverStateContainer());
        assertNotNull(Neodymium.getWebDriverStateContainer().getWebDriver());
    }
}
