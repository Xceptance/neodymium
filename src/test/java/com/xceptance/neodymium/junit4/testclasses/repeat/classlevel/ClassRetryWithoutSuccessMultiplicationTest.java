package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;
import com.xceptance.neodymium.util.Neodymium;
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
        if (DataUtils.asString("testId").equals("2") && Neodymium.getBrowserProfileName().contains("1500"))
        {
            Assert.fail("Fail " + i.incrementAndGet());
        }
    }
}
