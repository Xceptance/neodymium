package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.allure.AllureSelenideListenerIsActiveForJava;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberValidateAllureSelenideListenerIsActive;

public class AllureSelenideListenerTest extends NeodymiumTest
{
    @Test
    public void testAllureSelenideListenerIsActiveForCucumber()
    {
        Result result = run(CucumberValidateAllureSelenideListenerIsActive.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testAllureSelenideListenerIsActiveForJava()
    {
        Result result = run(AllureSelenideListenerIsActiveForJava.class);
        checkPass(result, 1, 0);
    }
}
