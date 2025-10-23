package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class InstantiateDtoViaJsonPathInAnnotation
{
    @DataItem("$.user")
    private User user;

    @DataItem("$.user.contacts.friends[2]")
    private User friend;

    @Test
    public void test1()
    {
        Assert.assertEquals("john" + Neodymium.getData().asString("testId") + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + Neodymium.getData().asString("testId"), user.getPassword());
    }

    @Test
    public void test2()
    {
        Assert.assertEquals("friend.of.john" + Neodymium.getData().asString("testId") + "@varmail.de", friend.getEmail());
        Assert.assertEquals("neodymium_friend_of_john" + Neodymium.getData().asString("testId"), friend.getPassword());
    }
}
