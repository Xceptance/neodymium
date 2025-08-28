package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.allure.AllureSelenideListenerIsActiveForJava;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentDataOrderTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionCircularReferenceTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionTest;

public class AllureCustomEnvironmentTest extends NeodymiumTest
{
    @Test
    public void testCustomEnvironmentDataOrderTest()
    {
        Result result = JUnitCore.runClasses(CustomEnvironmentDataOrderTest.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionCircularReferenceTest()
    {
        Result result = JUnitCore.runClasses(CustomEnvironmentSubstitutionCircularReferenceTest.class);
        checkPass(result, 1, 0);
    }
    
    @Test
    public void testCustomEnvironmentSubstitutionTest()
    {
        Result result = JUnitCore.runClasses(CustomEnvironmentSubstitutionTest.class);
        checkPass(result, 1, 0);
    }
}
