package com.xceptance.neodymium.junit4.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@Retry(exceptions =
{
    "Fail Parent"
})
public abstract class RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @BeforeClass
    public static void beforeClassParent()
    {
        reset();
    }

    public static void reset()
    {
        i.set(0);
    }

    @Test
    public void parentTest()
    {
        Assert.fail("Fail Parent "+i.incrementAndGet());
    }
}
