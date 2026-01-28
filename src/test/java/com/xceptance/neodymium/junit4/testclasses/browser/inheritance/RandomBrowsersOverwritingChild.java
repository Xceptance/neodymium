package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.Test;

import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.retry.Retry;
@Retry(exceptions = "Could not start a new session. Response code 500")
@RandomBrowsers(3)
public class RandomBrowsersOverwritingChild extends RandomBrowsersParent
{
    @Test
    public void test()
    {
    }
}
