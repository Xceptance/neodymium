package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryStopOnSuccessTest
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void test()
    {
        if (i.incrementAndGet() < 2)
        {
            Assert.fail("Fail");
        }
    }
}
