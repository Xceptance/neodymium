package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;
import com.xceptance.neodymium.util.Neodymium;
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
        if (DataUtils.asString("testId").equals("2") && Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + i.incrementAndGet());
        }
    }
}
