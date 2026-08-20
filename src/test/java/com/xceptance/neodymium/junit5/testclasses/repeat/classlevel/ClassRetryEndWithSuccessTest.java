package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryEndWithSuccessTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
        if (i.incrementAndGet() < 3)
        {
            Assert.fail("Fail");
        }
    }
}
