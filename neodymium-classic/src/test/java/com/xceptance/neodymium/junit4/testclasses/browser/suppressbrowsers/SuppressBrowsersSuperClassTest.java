package com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

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
