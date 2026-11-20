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
package com.xceptance.neodymium.junit4.testclasses.datautils;

import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        Assert.assertEquals("junit-5v2h60pqee@varmail.de", email);
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
        Assert.assertEquals("i-S-3Y*7hqZ4", password);
    }
}
