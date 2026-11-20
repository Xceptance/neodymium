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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.popupblocker.PopupBlockerTestclass;

public class PopupBlockerTest extends NeodymiumTest
{
    @Test
    public void testPopupBlocker()
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.popup.custom", "#myPopUp1");
        properties.put("neodymium.popup.second", "#myPopUp2");
        properties.put("neodymium.popup.third", "#myPopUp3");
        properties.put("neodymium.popup.customWithQuotes", "[data-testid='closeIcon']");

        addPropertiesForTest("temp-PopupBlockerTest-neodymium.properties", properties);

        Result result = run(PopupBlockerTestclass.class);
        checkPass(result, 7, 0);
    }
}
