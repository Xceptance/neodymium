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
package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.Configuration;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class SelenideConfigurationShortcuts
{
    @Test
    public void validateSoftAssertion()
    {
        Assert.assertEquals(Configuration.assertionMode, AssertionMode.STRICT);

        Neodymium.softAssertions(true);
        Assert.assertEquals(Configuration.assertionMode, AssertionMode.SOFT);

        Neodymium.softAssertions(false);
        Assert.assertEquals(Configuration.assertionMode, AssertionMode.STRICT);
    }

    @Test
    public void validateClickViaJs()
    {
        Assert.assertEquals(Configuration.clickViaJs, false);

        Neodymium.clickViaJs(true);
        Assert.assertEquals(Configuration.clickViaJs, true);

        Neodymium.clickViaJs(false);
        Assert.assertEquals(Configuration.clickViaJs, false);
    }

    @Test
    public void validateFastSetValue()
    {
        Assert.assertEquals(Configuration.fastSetValue, false);

        Neodymium.fastSetValue(true);
        Assert.assertEquals(Configuration.fastSetValue, true);

        Neodymium.fastSetValue(false);
        Assert.assertEquals(Configuration.fastSetValue, false);
    }

    @Test
    public void validateTimeout()
    {
        Assert.assertEquals(Configuration.timeout, 3000);

        Neodymium.timeout(1000);
        Assert.assertEquals(Configuration.timeout, 1000);

        Neodymium.timeout(2000);
        Assert.assertEquals(Configuration.timeout, 2000);
    }
}
