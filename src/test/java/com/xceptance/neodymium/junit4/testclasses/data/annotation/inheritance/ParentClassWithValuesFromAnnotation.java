package com.xceptance.neodymium.junit4.testclasses.data.annotation.inheritance;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public abstract class ParentClassWithValuesFromAnnotation
{
    @DataItem
    protected String name;

    @DataItem
    protected String testId;
}
