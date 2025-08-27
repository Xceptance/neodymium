package com.xceptance.neodymium.junit4.testclasses.repeat.methodlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Retry(exceptions =
{
  "Parent fail"
})
@RunWith(NeodymiumRunner.class)
public class MethodRepeatOnFailureTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Test
    @Retry(maxNumberOfRetries = 4, exceptions =
    {
      "Child fail"
    })
    public void testVisitingHomepage()
    {
        Assert.fail(val.getAndIncrement() < 3 ? "Child fail" : "Parent fail");
    }
}
