package com.xceptance.neodymium.junit4.tests;

import com.xceptance.neodymium.junit4.testclasses.softassertion.UseSoftAssertions;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.After;
import org.junit.Test;

import org.junit.runner.Result;

public class SoftAssertionTest extends NeodymiumTest
{
    @Test
    public void validateSoftAssertion()
    {
        Result result = run(UseSoftAssertions.class);
        checkFail(result, 1, 0, 1);
    }

    @After
    public void resetAssertionMode()
    {
        Neodymium.softAssertions(false);
    }
}
