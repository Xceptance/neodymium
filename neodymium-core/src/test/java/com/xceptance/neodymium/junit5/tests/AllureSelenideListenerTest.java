package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.allure.AllureSelenideListenerIsActiveForJava;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class AllureSelenideListenerTest extends AbstractNeodymiumTest
{

    @Test
    public void testAllureSelenideListenerIsActiveForJava()
    {
        NeodymiumTestExecutionSummary summary = run(AllureSelenideListenerIsActiveForJava.class);
        checkPass(summary, 1, 0);
    }
}
