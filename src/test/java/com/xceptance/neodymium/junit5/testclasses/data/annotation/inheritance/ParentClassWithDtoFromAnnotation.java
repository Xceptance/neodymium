package com.xceptance.neodymium.junit5.testclasses.data.annotation.inheritance;

import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.User;

public abstract class ParentClassWithDtoFromAnnotation
{
    @DataItem
    protected User user;
}
