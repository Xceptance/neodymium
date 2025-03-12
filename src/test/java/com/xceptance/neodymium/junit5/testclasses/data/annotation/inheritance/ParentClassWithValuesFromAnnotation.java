package com.xceptance.neodymium.junit5.testclasses.data.annotation.inheritance;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataItem;

@SuppressBrowsers
public abstract class ParentClassWithValuesFromAnnotation
{
    @DataItem
    protected String name;

    @DataItem
    protected String testId;
}
