package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@Retry(exceptions =
{
  "Fail"
})
@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
public class ClassRetryBrowserCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @NeodymiumTest
    public void testVisitingHomepage()
    {
        if (Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + val.incrementAndGet());
        }
    }
}
