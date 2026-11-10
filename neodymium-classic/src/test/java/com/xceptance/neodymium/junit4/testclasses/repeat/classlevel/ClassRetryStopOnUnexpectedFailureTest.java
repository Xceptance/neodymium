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
  "Fail 1"
})
public class ClassRetryStopOnUnexpectedFailureTest
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void test()
    {
        Assert.fail("Fail " + i.incrementAndGet());
    }
}
