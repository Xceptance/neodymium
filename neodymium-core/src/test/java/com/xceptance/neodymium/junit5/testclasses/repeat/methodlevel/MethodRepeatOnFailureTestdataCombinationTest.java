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
package com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel;

import org.neodymium.common.retry.Retry;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

public class MethodRepeatOnFailureTestdataCombinationTest
{
    public static AtomicInteger val = new AtomicInteger(0);

    @Retry(exceptions =
    {
      "Fail"
    })
    @NeodymiumTest
    public void testVisitingHomepage()
    {
        if (Neodymium.getData().asString("testId").equals("2"))
        {
            Assert.fail("Fail " + val.incrementAndGet());
        }
    }
}
