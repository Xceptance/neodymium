package com.xceptance.neodymium.testclasses.repeat.methodlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.statement.repeat.RepeatOnFailure;

@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
@RunWith(NeodymiumRunner.class)
public class MethodRepeatOnFailureBrowserCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Test
    @RepeatOnFailure(10)
    public void testVisitingHomepage()
    {
        int i = val.getAndIncrement();
        if (val.get() == 6)
        {
            val.set(0);
        }
        Assert.assertFalse("Produce test failure number " + i, i < 5);
    }
}
