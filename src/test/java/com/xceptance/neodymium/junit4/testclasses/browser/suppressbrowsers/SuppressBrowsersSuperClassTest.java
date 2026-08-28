package com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class SuppressBrowsersSuperClassTest
{
    @Test
    public void test(){
        assertNull(Neodymium.getDriver());
        assertNull(Neodymium.getWebDriverStateContainer());
    }
}
