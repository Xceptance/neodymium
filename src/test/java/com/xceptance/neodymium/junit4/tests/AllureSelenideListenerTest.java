package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.allure.AllureSelenideListenerIsActiveForJava;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberValidateAllureSelenideListenerIsActive;

public class AllureSelenideListenerTest extends NeodymiumTest
{
    @Test
    public void testAllureSelenideListenerIsActiveForCucumber()
    {
        Result result = JUnitCore.runClasses(CucumberValidateAllureSelenideListenerIsActive.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testAllureSelenideListenerIsActiveForJava()
    {
        Result result = JUnitCore.runClasses(AllureSelenideListenerIsActiveForJava.class);
        checkPass(result, 1, 0);
    }
}
