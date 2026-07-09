package com.xceptance.neodymium.junit5.testclasses.allure;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.logevents.SelenideLogger;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class AllureSelenideListenerIsActiveForJava
{
    @NeodymiumTest
    public void testWaitingAnimationSelectorUnconfigured()
    {
        Assertions.assertTrue(SelenideLogger.hasListener("allure-selenide-java"), " AllureSelenide listener is not attached");
    }
}
