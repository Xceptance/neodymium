package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.neodymium.common.testdata.DataItem;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.junit.Assert;

public class InstantiateDtoViaAnnotation
{
    @DataItem
    private User user;

    @NeodymiumTest
    public void test1()
    {
        Assert.assertEquals("john" + Neodymium.getData().asString("testId") + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + Neodymium.getData().asString("testId"), user.getPassword());
    }
}
