package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.Test;

import com.xceptance.neodymium.common.retry.Retry;
@Retry(exceptions = "Could not start a new session. Response code 500")
public class RandomBrowsersChild extends RandomBrowsersParent
{
    @Test
    public void test()
    {
    }
}
