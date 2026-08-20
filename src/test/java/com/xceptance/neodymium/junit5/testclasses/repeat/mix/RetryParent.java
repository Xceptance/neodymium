package com.xceptance.neodymium.junit5.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;
@Browser("Chrome_headless")
@Retry(exceptions =
{
    "Fail Parent"
})
public abstract class RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void parentTest()
    {
        Assert.fail("Fail Parent "+i.incrementAndGet());
    }
}
