package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;
@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
@Retry(exceptions =
{
  "Fail"
})
public class ClassRetryWithoutSuccessMultiplicationTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
        if (Neodymium.getData().asString("testId").equals("2") && Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + i.incrementAndGet());
        }
    }
}
