package com.xceptance.neodymium.junit4.testclasses.data.annotation.inheritance;

import org.junit.Assert;
import org.junit.Test;

import com.xceptance.neodymium.common.testdata.DataItem;

public class ChildInheritingDtoFromAnnotation extends ParentClassWithDtoFromAnnotation
{
    @DataItem
    private String testId;

    @Test
    public void test1()
    {
        Assert.assertEquals("john" + testId + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + testId, user.getPassword());
    }
}
