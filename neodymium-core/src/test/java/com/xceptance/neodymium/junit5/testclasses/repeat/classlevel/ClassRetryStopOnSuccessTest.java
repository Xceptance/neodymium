package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryStopOnSuccessTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
        if (i.incrementAndGet() < 2)
        {
            Assert.fail("Fail");
        }
    }
}
