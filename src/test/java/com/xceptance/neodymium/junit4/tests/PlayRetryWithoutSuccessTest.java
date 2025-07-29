package com.xceptance.neodymium.junit4.tests;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@Retry(exceptions =
{
  "HERE"
})
public class PlayRetryWithoutSuccessTest
{
    static AtomicInteger i = new AtomicInteger();

    @Before
    public void before()
    {
        Selenide.open("https://www.xceptance.com/de/");
    }

    @Test
    public void test()
    {
        Selenide.open("https://github.com/Xceptance/neodymium-example");
        Assert.fail("HERE " + i.incrementAndGet());
    }

    @After
    public void after()
    {
        Selenide.open("https://www.xceptance.com/en/");
    }
}
