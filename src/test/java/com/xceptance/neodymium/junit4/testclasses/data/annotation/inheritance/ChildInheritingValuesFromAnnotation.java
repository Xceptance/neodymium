package com.xceptance.neodymium.junit4.testclasses.data.annotation.inheritance;

import org.junit.Assert;
import org.junit.Test;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataItem;

@SuppressBrowsers
public class ChildInheritingValuesFromAnnotation extends ParentClassWithValuesFromAnnotation
{
    @DataItem
    private String testId;

    @Test
    public void test1()
    {
        Assert.assertEquals("John" + testId, name);
    }
}
