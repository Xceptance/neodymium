/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit4.testclasses.ai;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class AiBrowserDataResolutionTest
{
    @Test
    @DataSet(id = "NestedObjectTest")
    public void testDeepNestedResolution()
    {
        // Deep nested property (3 layers: user -> profile -> preferences -> theme)
        String template = "Theme is ${user.profile.preferences.theme}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Theme is dark", resolved);
    }

    @Test
    @DataSet(id = "NestedObjectTest")
    public void testJsonPathNotation()
    {
        // json path notation: [key]
        String template = "Theme is ${user[profile][preferences][theme]}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Theme is dark", resolved);
    }

    @Test
    @DataSet(id = "NestedObjectTest")
    public void testNestedPlaceholderResolution()
    {
        String template = "Greeting: ${user.profile.preferences.greetingTemplate}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Greeting: Welcome to dark theme!", resolved);
    }

    @Test
    @DataSet(id = "NestedObjectTest")
    public void testSimpleResolution()
    {
        String template = "Simple ${reference}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Simple value", resolved);
    }
}
