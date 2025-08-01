package com.xceptance.neodymium.junit4.testclasses.repeat.mix;

import org.junit.Assert;
import org.junit.Test;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;

@Browser("Chrome_headless")
@Retry(exceptions =
{
  "HERE"
})
public class OverwriteRetryTest extends RetryParent
{
    @Test
    public void childTest()
    {
        Assert.fail("NOT_HERE");
    }
    
    @Test
    public void parentTest()
    {
        Assert.fail("NOT_HERE");
    }
}
