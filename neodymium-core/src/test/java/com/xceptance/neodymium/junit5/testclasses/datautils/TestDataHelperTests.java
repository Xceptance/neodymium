/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit5.testclasses.datautils;

import org.neodymium.common.testdata.TestData;
import org.neodymium.junit5.NeodymiumTest;
import org.junit.jupiter.api.Assertions;

public class TestDataHelperTests
{
    @NeodymiumTest
    public void testRandomEmail()
    {
        String email = TestData.randomEmail();
        Assertions.assertNotNull(email);
        Assertions.assertEquals(27, email.length());
        Assertions.assertTrue(email.startsWith("junit-"));
        Assertions.assertTrue(email.endsWith("@varmail.de"));
    }

    @NeodymiumTest
    public void testFixedRandomEmail()
    {
        String email = TestData.randomEmail();
        // test fixed random
        Assertions.assertEquals("junit-5v2h60pqee@varmail.de", email);
    }

    @NeodymiumTest
    public void testRandomPassword()
    {
        String password = TestData.randomPassword();
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
        String password = TestData.randomPassword();
        // test fixed random
        Assertions.assertEquals("i-S-3Y*7hqZ4", password);
    }
}
