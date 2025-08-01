package com.xceptance.neodymium.junit4.testclasses.repeat.methodlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
@RunWith(NeodymiumRunner.class)
public class MethodRepeatOnFailureBrowserCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Retry(exceptions =
    {
      "Fail"
    })
    @Test
    public void testWithRetry()
    {
        if (Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + val.get());
        }
    }

    @Test
    public void testWithoutRetry()
    {
        if (Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + val.get());
        }
    }
}
