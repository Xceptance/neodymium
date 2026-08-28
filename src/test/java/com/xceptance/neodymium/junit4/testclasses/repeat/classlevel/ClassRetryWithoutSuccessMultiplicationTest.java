package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryWithoutSuccessMultiplicationTest
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void test()
    {
        if (Neodymium.getData().asString("testId").equals("2") && Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + i.incrementAndGet());
        }
    }
}
