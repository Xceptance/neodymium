package com.xceptance.neodymium.junit5.testclasses.datautils;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

public class DataUtilsHelperTests
{
    @NeodymiumTest
    public void testRandomEmail()
    {
        String email = DataUtils.randomEmail();
        Assertions.assertNotNull(email);
        Assertions.assertEquals(27, email.length());
        Assertions.assertTrue(email.startsWith("junit-"));
        Assertions.assertTrue(email.endsWith("@varmail.de"));
    }

    @NeodymiumTest
    public void testFixedRandomEmail()
    {
        String email = DataUtils.randomEmail();
        // test fixed random
        Assertions.assertEquals("junit-5v2h60pqee@varmail.de", email);
    }

    @NeodymiumTest
    public void testRandomPassword()
    {
        String password = DataUtils.randomPassword();
        Assertions.assertNotNull(password);
        Assertions.assertEquals(12, password.length());

        Assertions.assertEquals(3, countCharsInString(password, "abcdefghijklmnopqrstuvwxyz"));
        Assertions.assertEquals(3, countCharsInString(password, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        Assertions.assertEquals(3, countCharsInString(password, "0123456789"));
        Assertions.assertEquals(3, countCharsInString(password, "#-_*"));
    }

    private long countCharsInString(String subject, String charsToCount)
    {
        return subject.chars().filter(c -> charsToCount.contains(String.valueOf((char) c))).count();
    }

    @NeodymiumTest
    public void testFixedRandomPassword()
    {
        String password = DataUtils.randomPassword();
        // test fixed random
        Assertions.assertEquals("i_S_3Y-7hqZ4", password);
    }
}
