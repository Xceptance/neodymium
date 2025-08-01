package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Retry(exceptions =
{
  "Fail"
})
@Browser("Chrome_headless")
public class ClassNoRetryOnUnexpectedErrorTest
{
    @Test
    public void test()
    {
        Assert.fail("Shoul not be retried");
    }
}
