package com.xceptance.neodymium.junit5.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
@Retry(exceptions =
{
    "Fail Parent"
})
public abstract class RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @BeforeAll
    public static void beforeAllParent()
    {
        reset();
    }

    public static void reset()
    {
        i.set(0);
    }

    @NeodymiumTest
    public void parentTest()
    {
        Assert.fail("Fail Parent "+i.incrementAndGet());
    }
}
