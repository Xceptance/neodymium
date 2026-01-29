package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.Test;

import com.xceptance.neodymium.common.browser.RandomBrowsers;
@RandomBrowsers(3)
public class RandomBrowsersOverwritingChild extends RandomBrowsersParent
{
    @Test
    public void test()
    {
    }
}
