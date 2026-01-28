package com.xceptance.neodymium.junit5.testclasses.browser.inheritance;

import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
@Retry(exceptions = "Could not start a new session. Response code 500")
@RandomBrowsers(3)
public class RandomBrowsersOverwritingChild extends RandomBrowsersParent
{
    @NeodymiumTest
    public void test()
    {
    }
}
