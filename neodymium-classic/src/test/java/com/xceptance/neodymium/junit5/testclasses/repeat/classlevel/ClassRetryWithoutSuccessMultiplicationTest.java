package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
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
