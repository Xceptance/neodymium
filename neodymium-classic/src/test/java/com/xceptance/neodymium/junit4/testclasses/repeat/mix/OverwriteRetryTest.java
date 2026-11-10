package com.xceptance.neodymium.junit4.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.xceptance.neodymium.common.retry.Retry;

@Retry(exceptions =
{
	"Fail Child"
})
public class OverwriteRetryTest extends RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void childTest()
    {
        Assert.fail("Fail Child "+i.incrementAndGet());
    }
}
