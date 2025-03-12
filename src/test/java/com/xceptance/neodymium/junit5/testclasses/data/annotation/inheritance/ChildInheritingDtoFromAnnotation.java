package com.xceptance.neodymium.junit5.testclasses.data.annotation.inheritance;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
public class ChildInheritingDtoFromAnnotation extends ParentClassWithDtoFromAnnotation
{
    @DataItem
    private String testId;

    @NeodymiumTest
    public void test1()
    {
        Assert.assertEquals("john" + testId + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + testId, user.getPassword());
    }
}
