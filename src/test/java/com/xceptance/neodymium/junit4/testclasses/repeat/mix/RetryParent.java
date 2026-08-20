package com.xceptance.neodymium.junit4.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@Retry(exceptions =
{
    "Fail Parent"
})
public abstract class RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void parentTest()
    {
        Assert.fail("Fail Parent "+i.incrementAndGet());
    }
}
