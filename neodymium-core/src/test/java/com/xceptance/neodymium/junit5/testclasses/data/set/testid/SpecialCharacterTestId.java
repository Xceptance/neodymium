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
package com.xceptance.neodymium.junit5.testclasses.data.set.testid;

import org.junit.jupiter.api.Assertions;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

public class SpecialCharacterTestId
{
    static int dataSet = 1;

    @NeodymiumTest
    public void testParanthesis() throws Exception
    {
        switch (dataSet)
        {
            case 1:
                Assertions.assertEquals("'", Neodymium.dataValue("testId"));
                break;

            case 2:
                Assertions.assertEquals("''", Neodymium.dataValue("testId"));
                break;

            case 3:
                Assertions.assertEquals("(", Neodymium.dataValue("testId"));
                break;

            case 4:
                Assertions.assertEquals(")", Neodymium.dataValue("testId"));
                break;

            case 5:
                Assertions.assertEquals("()", Neodymium.dataValue("testId"));
                break;

            case 6:
                Assertions.assertEquals(")(", Neodymium.dataValue("testId"));
                break;

            case 7:
                Assertions.assertEquals("(a)", Neodymium.dataValue("testId"));
                break;

            default:
                Assertions.fail("uncovered test");
                break;
        }
        dataSet++;
    }
}
