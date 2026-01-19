package com.xceptance.neodymium.junit4.testclasses.filtering;

import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import io.cucumber.java.it.Data;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class TestCaseFiltering
{
    @Test
    @Data("id=executable")
    public void shouldBeExecuted()
    {
        Assert.assertTrue("This test should be executed", true);
        Assert.assertEquals("This test should only be executed for data set with id 'executable'", "executable",
                            Neodymium.getData().asString("testId"));
    }

    @Test
    @SuppressDataSets
    public void shouldNotBeExecuted()
    {
        Assert.assertTrue("This test should not be executed", false);
    }

    @Test
    public void shouldBeExecutedForDataSetWithExecutableId()
    {
        Assert.assertEquals("This test should only be executed for data set with id 'executable'", "executable",
                            Neodymium.getData().asString("testId"));
    }
}
