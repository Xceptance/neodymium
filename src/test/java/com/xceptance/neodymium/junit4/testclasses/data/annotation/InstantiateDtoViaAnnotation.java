package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.neodymium.common.testdata.DataItem;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class InstantiateDtoViaAnnotation
{
    @DataItem
    private User user;

    @Test
    public void test1()
    {
        Assert.assertEquals("john" + Neodymium.getData().asString("testId") + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + Neodymium.getData().asString("testId"), user.getPassword());
    }
}
