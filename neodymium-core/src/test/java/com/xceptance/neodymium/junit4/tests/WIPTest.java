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
package com.xceptance.neodymium.junit4.tests;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.wip.WIPAnnotationChildTest;
import com.xceptance.neodymium.junit4.testclasses.wip.WIPAnnotationTest;
import com.xceptance.neodymium.junit4.testclasses.wip.WIPMultiplicationTest;
import org.neodymium.util.Neodymium;

public class WIPTest extends NeodymiumTest
{
    @Before
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
        Neodymium.configuration().setProperty("neodymium.workInProgress", "true");
    }

    @Test
    public void testWIPAnnotation() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: "
        };
        checkDescription(WIPAnnotationTest.class, expected);

        Result result = run(WIPAnnotationTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testWIPAnnotationChild() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: ",
          "third :: "
        };
        checkDescription(WIPAnnotationChildTest.class, expected);

        Result result = run(WIPAnnotationChildTest.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testWIPMultiplication() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: 1 1 / 2 :: Browser Chrome_headless :: ",
          "first :: 2 2 / 2 :: Browser Chrome_headless :: ",
          "first :: 1 1 / 2 :: Browser Chrome_1500x1000_headless :: ",
          "first :: 2 2 / 2 :: Browser Chrome_1500x1000_headless :: ",
        };
        checkDescription(WIPMultiplicationTest.class, expected);
        Result result = run(WIPMultiplicationTest.class);
        checkPass(result, 4, 0);
    }
}
