package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryWithoutSuccessInAfterTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
    }

    @AfterEach
    public void after()
    {
        Assert.fail("Fail " + i.incrementAndGet());
    }
}
