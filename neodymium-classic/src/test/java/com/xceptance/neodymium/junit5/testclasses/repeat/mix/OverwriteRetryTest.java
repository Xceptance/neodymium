package com.xceptance.neodymium.junit5.testclasses.repeat.mix;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
	"Fail Child"
})
public class OverwriteRetryTest extends RetryParent
{
    static AtomicInteger i = new AtomicInteger();

    @BeforeAll
    public static void beforeAll()
    {
        reset();
    }

    public static void reset()
    {
        i.set(0);
    }

    @NeodymiumTest
    public void childTest()
    {
        Assert.fail("Fail Child "+i.incrementAndGet());
    }
}
