package com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
  "Parent fail"
})
public class MethodRepeatOnFailureTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @NeodymiumTest
    @Retry(maxNumberOfRetries = 4, exceptions =
    {
      "Child fail"
    })
    public void testVisitingHomepage()
    {
        Assert.fail(val.getAndIncrement() < 3 ? "Child fail" : "Parent fail");
    }
}
