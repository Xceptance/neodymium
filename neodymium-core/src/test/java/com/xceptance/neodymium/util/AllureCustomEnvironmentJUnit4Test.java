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
package com.xceptance.neodymium.util;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentDataOrderTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionCircularReferenceTest;
import com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata.CustomEnvironmentSubstitutionTest;

public class AllureCustomEnvironmentJUnit4Test extends NeodymiumTest
{
    @Test
    public void testCustomEnvironmentDataOrderTest()
    {
        AllureAddons.customDataAdded = false;
        Result result = run(CustomEnvironmentDataOrderTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionCircularReferenceTest()
    {
        AllureAddons.customDataAdded = false;
        Result result = run(CustomEnvironmentSubstitutionCircularReferenceTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCustomEnvironmentSubstitutionTest()
    {
        AllureAddons.customDataAdded = false;
        Result result = run(CustomEnvironmentSubstitutionTest.class);
        checkPass(result, 1, 0);
    }
}
