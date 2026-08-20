package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryWithoutSuccessInAfterTest
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void test()
    {
    }

    @SuppressBrowsers
    @After
    public void after()
    {
        Assert.fail("Fail " + i.incrementAndGet());
    }
}
