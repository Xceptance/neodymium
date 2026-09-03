package com.xceptance.neodymium.junit4.testclasses.data.annotation.inheritance;

import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataItem;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.User;

@RunWith(NeodymiumRunner.class)
public abstract class ParentClassWithDtoFromAnnotation
{
    @DataItem
    protected User user;
}
