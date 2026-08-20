package com.xceptance.neodymium.junit4.testclasses.data.annotation.inheritance;

import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataItem;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public abstract class ParentClassWithValuesFromAnnotation
{
    @DataItem
    protected String name;

    @DataItem
    protected String testId;
}
