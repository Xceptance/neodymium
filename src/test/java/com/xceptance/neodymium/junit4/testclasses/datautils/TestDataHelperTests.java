package com.xceptance.neodymium.junit4.testclasses.datautils;

import com.xceptance.neodymium.common.testdata.TestData;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class TestDataHelperTests
{
    @Test
    public void testRandomEmail()
    {
        String email = TestData.randomEmail();
        Assert.assertNotNull(email);
        Assert.assertEquals(27, email.length());
        Assert.assertTrue(email.startsWith("junit-"));
        Assert.assertTrue(email.endsWith("@varmail.de"));
    }

    @Test
    public void testFixedRandomEmail()
    {
        String email = TestData.randomEmail();
        // test fixed random
        Assert.assertEquals("junit-01uh2qpree@varmail.de", email);
    }

    @Test
    public void testRandomPassword()
    {
        String password = TestData.randomPassword();
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
        String password = TestData.randomPassword();
        // test fixed random
        Assert.assertEquals("i_S_3Y-7hqZ4", password);
    }
}
