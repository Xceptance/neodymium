/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.data.set.testid;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

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
