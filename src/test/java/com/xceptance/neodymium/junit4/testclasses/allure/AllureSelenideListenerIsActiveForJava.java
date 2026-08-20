package com.xceptance.neodymium.junit4.testclasses.allure;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.logevents.SelenideLogger;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class AllureSelenideListenerIsActiveForJava
{
    @Test
    public void testWaitingAnimationSelectorUnconfiguredJUnit4()
    {
        Assert.assertTrue(" AllureSelenide listener is not attached", SelenideLogger.hasListener(NeodymiumRunner.LISTENER_NAME));
    }

}
