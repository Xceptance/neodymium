package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
  "Fail 1"
})
public class ClassRetryStopOnUnexpectedFailureTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
        Assert.fail("Fail " + i.incrementAndGet());
    }
}
