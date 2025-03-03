package com.xceptance.neodymium.junit4.testclasses.datautils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
public class DataUtilsHelperTests
{
    @Test
    public void testRandomEmail()
    {
        String email = DataUtils.randomEmail();
        Assert.assertNotNull(email);
        Assert.assertEquals(27, email.length());
        Assert.assertTrue(email.startsWith("junit-"));
        Assert.assertTrue(email.endsWith("@varmail.de"));
    }

    @Test
    public void testFixedRandomEmail()
    {
        String email = DataUtils.randomEmail();
        // test fixed random
        Assert.assertEquals("junit-lwtq5qha2z@varmail.de", email);
    }

    @Test
    public void testRandomPassword()
    {
        String password = DataUtils.randomPassword();
        Assert.assertNotNull(password);
        Assert.assertEquals(12, password.length());

        Assert.assertEquals(3, countCharsInString(password, "abcdefghijklmnopqrstuvwxyz"));
        Assert.assertEquals(3, countCharsInString(password, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        Assert.assertEquals(3, countCharsInString(password, "0123456789"));
        Assert.assertEquals(3, countCharsInString(password, "#-_*"));
    }

    private long countCharsInString(String subject, String charsToCount)
    {
        return subject.chars().filter(c -> charsToCount.contains(String.valueOf((char) c))).count();
    }

    @Test
    public void testFixedRandomPassword()
    {
        String password = DataUtils.randomPassword();
        // test fixed random
        Assert.assertEquals("i_S_3Y-7hqZ4", password);
    }
}
