package com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.JUnitCore;

public class CustomEnvironmentSubstitutionCircularReferenceTest extends AbstractNeodymiumTest
{
    @NeodymiumTest
    public void testPropertySubstitution()
    {

        NeodymiumTestExecutionSummary summary = run(com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.CustomEnvironmentPropertySubstitutionCircularReferenceTestClass.class);
        checkFail(summary, 1, 0, 1, "java.lang.RuntimeException: No browser setting for @AfterEach method 'after' was found. "
            + "If browser was suppressed for the test but is annotated with @StartNewBrowserForCleanUp because browser isrequired for the clean up,"
            + " please, use @Browser annotation to specify what browser is required for this clean up.");

        JUnitCore.runClasses(com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.CustomEnvironmentPropertySubstitutionCircularReferenceTestClass.class);
        Assertions.assertTrue(true,
                "This test is expected to fail due to circular property references in the setup phase.");
    }
}
