package com.xceptance.neodymium.util;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.CustomEnvironmentDataOrderTest;
import com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionCircularReferenceTest;
import com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class AllureCustomEnvironmentJUnit5Test extends AbstractNeodymiumTest
{
    @Test
    public void testCustomEnvironmentDataOrderTest()
    {
        AllureAddons.customDataAdded = false;
        NeodymiumTestExecutionSummary result = run(CustomEnvironmentDataOrderTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionCircularReferenceTest()
    {
        AllureAddons.customDataAdded = false;
        NeodymiumTestExecutionSummary result = run(CustomEnvironmentSubstitutionCircularReferenceTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionTest()
    {
        AllureAddons.customDataAdded = false;
        NeodymiumTestExecutionSummary result = run(CustomEnvironmentSubstitutionTest.class);
        checkPass(result, 1, 0);
    }
}
