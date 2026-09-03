package com.xceptance.neodymium.junit4.testclasses.repeat.methodlevel;

import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(NeodymiumRunner.class)
public class MethodRepeatOnFailureTestdataCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Retry(exceptions =
    {
      "Fail"
    })
    @Test
    public void testVisitingHomepage()
    {
        if (Neodymium.getData().asString("testId").equals("2"))
        {
            Assert.fail("Fail " + val.incrementAndGet());
        }
    }
}
