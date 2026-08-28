package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.neodymium.common.testdata.DataItem;
import org.neodymium.junit5.NeodymiumTest;
import org.junit.jupiter.api.Assertions;

public class InstantiateDtoViaYamlAnnotation
{
    @DataItem
    private User user;

    @NeodymiumTest
    public void testComplexYamlBinding()
    {
        Assertions.assertEquals("yamluser@example.com", user.getEmail());
        Assertions.assertEquals("yamlpass123", user.getPassword());
    }
}
