package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryWithoutSuccessInBeforeTest
{
    static AtomicInteger i = new AtomicInteger();

    @SuppressBrowsers
    @BeforeEach
    public void before()
    {
        Assert.fail("Fail " + i.incrementAndGet());
    }

    @NeodymiumTest
    public void test()
    {
        Assert.fail("Test method should not be executed");
    }
}
