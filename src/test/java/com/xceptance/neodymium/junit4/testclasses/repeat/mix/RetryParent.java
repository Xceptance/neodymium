package com.xceptance.neodymium.junit4.testclasses.repeat.mix;

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
  "HERE"
})
public abstract class RetryParent
{
    @Test
    public void parentTest()
    {
        Assert.fail("HERE");
    }
}
