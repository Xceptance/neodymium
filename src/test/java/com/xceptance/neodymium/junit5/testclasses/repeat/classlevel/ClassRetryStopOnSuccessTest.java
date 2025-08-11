package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryStopOnSuccessTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
        if (i.incrementAndGet() < 2)
        {
            Assert.fail("Fail");
        }
    }
}
