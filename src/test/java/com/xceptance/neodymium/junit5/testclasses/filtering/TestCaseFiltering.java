package com.xceptance.neodymium.junit5.testclasses.filtering;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

import io.cucumber.java.it.Data;

@SuppressBrowsers
public class TestCaseFiltering
{
    @NeodymiumTest
    @Data("id=executable")
    public void shouldBeExecuted()
    {
        Assert.assertTrue("This test should be executed", true);
        Assert.assertEquals("This test should only be executed for data set with id 'executable'", "executable",
                            DataUtils.asString("testId"));
    }

    @NeodymiumTest
    @SuppressDataSets
    public void shouldNotBeExecuted()
    {
        Assert.assertTrue("This test should not be executed", false);
    }

    @NeodymiumTest
    public void shouldBeExecutedForDataSetWithExecutableId()
    {
        Assert.assertEquals("This test should only be executed for data set with id 'executable'", "executable",
                            DataUtils.asString("testId"));
    }
}
