package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
public class InstantiateDtoViaAnnotation
{
    @DataItem
    private User user;

    @Test
    public void test1()
    {
        Assert.assertEquals("john" + DataUtils.asString("testId") + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + DataUtils.asString("testId"), user.getPassword());
    }
}
