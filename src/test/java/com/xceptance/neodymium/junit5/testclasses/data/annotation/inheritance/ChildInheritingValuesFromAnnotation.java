package com.xceptance.neodymium.junit5.testclasses.data.annotation.inheritance;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
public class ChildInheritingValuesFromAnnotation extends ParentClassWithValuesFromAnnotation
{
    @DataItem
    private String testId;

    @NeodymiumTest
    public void test1()
    {
        Assert.assertEquals("John" + testId, name);
    }
}
