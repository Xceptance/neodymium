package com.xceptance.neodymium.junit5.testclasses.browser.inheritance;

import org.neodymium.common.browser.RandomBrowsers;
import org.neodymium.junit5.NeodymiumTest;
@RandomBrowsers(3)
public class RandomBrowsersOverwritingChild extends RandomBrowsersParent
{
    @NeodymiumTest
    public void test()
    {
    }
}
