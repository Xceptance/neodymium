package com.xceptance.neodymium.junit4.testclasses.repeat.classlevel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;

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
