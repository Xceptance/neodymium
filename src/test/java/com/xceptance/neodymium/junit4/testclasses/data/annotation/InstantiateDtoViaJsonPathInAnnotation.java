package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class InstantiateDtoViaJsonPathInAnnotation
{
    @DataItem("$.user")
    private User user;

    @DataItem("$.user.contacts.friends[2]")
    private User friend;

    @Test
    public void test1()
    {
        Assert.assertEquals("john" + DataUtils.asString("testId") + "@varmail.de", user.getEmail());
        Assert.assertEquals("neodymium" + DataUtils.asString("testId"), user.getPassword());
    }

    @Test
    public void test2()
    {
        Assert.assertEquals("friend.of.john" + DataUtils.asString("testId") + "@varmail.de", friend.getEmail());
        Assert.assertEquals("neodymium_friend_of_john" + DataUtils.asString("testId"), friend.getPassword());
    }
}
