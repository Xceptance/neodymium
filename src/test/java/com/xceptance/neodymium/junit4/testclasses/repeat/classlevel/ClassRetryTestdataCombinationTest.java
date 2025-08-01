package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@Browser("Chrome_headless")
@Retry(exceptions =
{
  "Fail"
})
@RunWith(NeodymiumRunner.class)
public class ClassRetryTestdataCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Test
    public void testVisitingHomepage()
    {
        if (DataUtils.asString("testId").equals("2"))
        {
            Assert.fail("Fail " + val.get());
        }
    }
}
