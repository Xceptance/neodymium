package com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

@Browser("Chrome_headless")
public class MethodRepeatOnFailureTestdataCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Retry(exceptions =
    {
      "Fail"
    })
    @NeodymiumTest
    public void testVisitingHomepage()
    {
        if (Neodymium.getData().asString("testId").equals("2"))
        {
            Assert.fail("Fail " + val.incrementAndGet());
        }
    }
}
