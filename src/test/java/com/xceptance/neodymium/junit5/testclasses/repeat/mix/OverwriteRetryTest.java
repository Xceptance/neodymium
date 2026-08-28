package com.xceptance.neodymium.junit5.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
	"Fail Child"
})
public class OverwriteRetryTest extends RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void childTest()
    {
        Assert.fail("Fail Child "+i.incrementAndGet());
    }
}
