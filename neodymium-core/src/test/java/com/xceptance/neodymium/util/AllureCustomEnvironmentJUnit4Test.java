package com.xceptance.neodymium.util;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentDataOrderTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionCircularReferenceTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionTest;

public class AllureCustomEnvironmentJUnit4Test extends NeodymiumTest
{
    @Test
    public void testCustomEnvironmentDataOrderTest()
    {
        AllureAddons.customDataAdded = false;
        Result result = run(CustomEnvironmentDataOrderTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionCircularReferenceTest()
    {
        AllureAddons.customDataAdded = false;
        Result result = run(CustomEnvironmentSubstitutionCircularReferenceTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionTest()
    {
        AllureAddons.customDataAdded = false;
        Result result = run(CustomEnvironmentSubstitutionTest.class);
        checkPass(result, 1, 0);
    }
}
