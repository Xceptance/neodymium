/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package org.neodymium.ai.executor.selenide.selector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Unit tests for {@link PlaywrightBy}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaywrightByTest
{
    @Test
    public void testFactoryCreation()
    {
        final PlaywrightBy by = PlaywrightBy.of("button.primary >> text=Click");
        Assertions.assertNotNull(by);
        Assertions.assertEquals("button.primary >> text=Click", by.getRawSelector());
        Assertions.assertEquals(2, by.getChain().getSteps().size());
    }

    @Test
    public void testEqualityAndHashCode()
    {
        final PlaywrightBy by1 = PlaywrightBy.of("div[data-ai='xc123']");
        final PlaywrightBy by2 = PlaywrightBy.of("div[data-ai='xc123']");
        final PlaywrightBy by3 = PlaywrightBy.of("div[data-ai='xc456']");

        Assertions.assertEquals(by1, by2);
        Assertions.assertEquals(by1.hashCode(), by2.hashCode());
        Assertions.assertNotEquals(by1, by3);
    }

    @Test
    public void testSubclassOfSeleniumBy()
    {
        final By by = PlaywrightBy.of("#submit");
        Assertions.assertInstanceOf(By.class, by);
    }
}
