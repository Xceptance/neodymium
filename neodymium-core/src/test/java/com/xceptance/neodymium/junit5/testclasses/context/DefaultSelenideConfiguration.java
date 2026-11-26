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
package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Configuration;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class DefaultSelenideConfiguration
{
    @NeodymiumTest
    public void test1() throws Exception
    {
        Neodymium.timeout(1234);
        Assertions.assertEquals(1234, Configuration.timeout);

        Neodymium.fastSetValue(true);
        Assertions.assertEquals(true, Configuration.fastSetValue);

        Neodymium.clickViaJs(true);
        Assertions.assertEquals(true, Configuration.clickViaJs);
    }

    @NeodymiumTest
    public void test2() throws Exception
    {
        Assertions.assertEquals(3000, Configuration.timeout);
        Assertions.assertEquals(false, Configuration.fastSetValue);
        Assertions.assertEquals(false, Configuration.clickViaJs);
    }
}
