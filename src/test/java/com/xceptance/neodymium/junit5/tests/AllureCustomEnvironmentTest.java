package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.CustomEnvironmentDataOrderTest;
import com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionCircularReferenceTest;
import com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class AllureCustomEnvironmentTest extends AbstractNeodymiumTest
{
    @Test
    public void testCustomEnvironmentDataOrderTest()
    {
        NeodymiumTestExecutionSummary result = run(CustomEnvironmentDataOrderTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionCircularReferenceTest()
    {
        NeodymiumTestExecutionSummary result = run(CustomEnvironmentSubstitutionCircularReferenceTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionTest()
    {
        NeodymiumTestExecutionSummary result = run(CustomEnvironmentSubstitutionTest.class);
        checkPass(result, 1, 0);
    }
}
