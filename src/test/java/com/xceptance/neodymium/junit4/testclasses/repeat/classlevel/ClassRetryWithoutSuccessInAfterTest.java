package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
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

    @After
    public void after()
    {
        Assert.fail("Fail " + i.incrementAndGet());
    }
}
