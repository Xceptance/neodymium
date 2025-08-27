package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
@Retry
public class ClassRetryEmptyErrorMessageTest
{
    static AtomicInteger i = new AtomicInteger();

    @Test
    public void test()
    {
        Assert.fail();
    }
}
