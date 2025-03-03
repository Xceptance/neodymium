package com.xceptance.neodymium.junit5.tests;

import com.xceptance.neodymium.junit5.testclasses.softassertion.UseSoftAssertions;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SoftAssertionTest extends AbstractNeodymiumTest
{
    @Test
    public void validateSoftAssertion()
    {
        NeodymiumTestExecutionSummary summary = run(UseSoftAssertions.class);
        checkFail(summary, 1, 0, 1);
    }

    @AfterEach
    public void resetAssertionMode()
    {
        Neodymium.softAssertions(false);
    }
}
