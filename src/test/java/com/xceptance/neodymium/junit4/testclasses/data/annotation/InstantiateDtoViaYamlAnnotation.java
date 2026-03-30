package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataItem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class InstantiateDtoViaYamlAnnotation
{
    @DataItem
    private User user;

    @Test
    public void testComplexYamlBinding()
    {
        Assert.assertEquals("yamluser@example.com", user.getEmail());
        Assert.assertEquals("yamlpass123", user.getPassword());
    }
}
