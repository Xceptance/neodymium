package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
  "Fail"
})
public class ClassNoRetryOnUnexpectedErrorTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
    	Assert.assertTrue("Test should be run only once", i.incrementAndGet()==1);
        Assert.fail("Shoul not be retried");
    }
}
