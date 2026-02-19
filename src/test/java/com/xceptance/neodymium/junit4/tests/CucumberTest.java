package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTag;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTagFail;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTestData;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTestDataFail;

public class CucumberTest extends NeodymiumTest
{
    @Test
    public void testSetBrowserViaTestData() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTestData.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSetBrowserViaTestDataFail() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTestDataFail.class);
        checkFail(result, 1, 0, 1);
    }

    @Test
    public void testSetBrowserViaTag() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTag.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSetBrowserViaTagFail() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTagFail.class);
        checkFail(result, 1, 0, 1);
    }
}
