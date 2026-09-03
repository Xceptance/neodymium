package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForSetUp;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

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
    @BeforeEach
    public void before()
    {
        Assert.assertTrue("Separate browser for setup was not started", Neodymium.getBrowserProfileName().contains("1500"));
    }

    @NeodymiumTest
    public void test()
    {
        Assert.fail("Fail "+i.incrementAndGet());
    }
}
