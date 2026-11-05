package com.xceptance.neodymium.junit5.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
@Retry(maxNumberOfRetries = 4, exceptions =
{
  "Fail"
})
public class PlayRetryWithoutSuccessTest
{
    static AtomicInteger i = new AtomicInteger();

    @NeodymiumTest
    public void test()
    {
        Selenide.open("https://github.com/Xceptance/neodymium-example");
        Assert.fail("Fail " + i.incrementAndGet());
    }
}
