package com.xceptance.neodymium.junit5.testclasses.allure;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.logevents.SelenideLogger;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumRunner;
import org.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class AllureSelenideListenerIsActiveForJava
{
    @NeodymiumTest
    public void testWaitingAnimationSelectorUnconfigured()
    {
        Assertions.assertTrue(SelenideLogger.hasListener(NeodymiumRunner.LISTENER_NAME), " AllureSelenide listener is not attached");
    }
}
