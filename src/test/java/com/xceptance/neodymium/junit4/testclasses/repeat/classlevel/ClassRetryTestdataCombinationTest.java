package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@Retry(exceptions =
{
  "Fail"
})
@RunWith(NeodymiumRunner.class)
public class ClassRetryTestdataCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Test
    public void testVisitingHomepage()
    {
        if (Neodymium.getData().asString("testId").equals("2"))
        {
            Assert.fail("Fail " + val.incrementAndGet());
        }
    }
}
