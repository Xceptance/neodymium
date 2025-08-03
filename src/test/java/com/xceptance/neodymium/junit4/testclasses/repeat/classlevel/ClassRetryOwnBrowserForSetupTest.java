package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryOwnBrowserForSetupTest
{
    static AtomicInteger i = new AtomicInteger();

    @StartNewBrowserForSetUp
    @Browser("Chrome_1500x1000_headless")
    @Before
    public void before()
    {
        Assert.assertTrue("Separate browser for setup was not started", Neodymium.getBrowserProfileName().contains("1500"));
    }

    @Test
    public void test()
    {
        Assert.fail("Fail "+i.incrementAndGet());
    }
}
