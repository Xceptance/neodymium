package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

@Browser("Chrome_headless")
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryTestdataCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @NeodymiumTest
    public void testVisitingHomepage()
    {
        if (Neodymium.getData().asString("testId").equals("2"))
        {
            Assert.fail("Fail " + val.incrementAndGet());
        }
    }
}
