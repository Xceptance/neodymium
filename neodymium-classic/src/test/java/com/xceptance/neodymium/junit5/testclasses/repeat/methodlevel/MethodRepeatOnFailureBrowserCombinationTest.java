package com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
public class MethodRepeatOnFailureBrowserCombinationTest
{
    public static AtomicInteger val1 = new AtomicInteger(0);

    public static AtomicInteger val2 = new AtomicInteger(0);

    @Retry(exceptions =
    {
      "Fail"
    })
    @NeodymiumTest
    public void testWithRetry()
    {
        if (Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + val1.incrementAndGet());
        }
    }

    @NeodymiumTest
    public void testWithoutRetry()
    {
        if (Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + val2.incrementAndGet());
        }
    }
}
