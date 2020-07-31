package com.xceptance.neodymium.tests;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.testclasses.cucumber.CucumberSetBrowserViaTag;
import com.xceptance.neodymium.testclasses.cucumber.CucumberSetBrowserViaTagFail;
import com.xceptance.neodymium.testclasses.cucumber.CucumberSetBrowserViaTestData;
import com.xceptance.neodymium.testclasses.cucumber.CucumberSetBrowserViaTestDataFail;

public class CucumberTest extends NeodymiumTest
{
    @Test
    public void testSetBrowserViaTestData() throws Exception
    {
        Result result = JUnitCore.runClasses(CucumberSetBrowserViaTestData.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSetBrowserViaTestDataFail() throws Exception
    {
        Result result = JUnitCore.runClasses(CucumberSetBrowserViaTestDataFail.class);
        checkFail(result, 1, 0, 1);
    }

    @Test
    public void testSetBrowserViaTag() throws Exception
    {
        Result result = JUnitCore.runClasses(CucumberSetBrowserViaTag.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSetBrowserViaTagFail() throws Exception
    {
        Result result = JUnitCore.runClasses(CucumberSetBrowserViaTagFail.class);
        checkFail(result, 1, 0, 1);
    }
}
