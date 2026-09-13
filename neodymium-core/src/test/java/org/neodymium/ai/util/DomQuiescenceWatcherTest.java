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
package org.neodymium.ai.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying {@link DomQuiescenceWatcher} utility contract, safety guards,
 * and lifecycle execution when WebDriver is not active.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
final class DomQuiescenceWatcherTest
{
    @Test
    void testUtilityClassProperties() throws Exception
    {
        final Constructor<DomQuiescenceWatcher> constructor = DomQuiescenceWatcher.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor must be private");

        constructor.setAccessible(true);
        final DomQuiescenceWatcher instance = constructor.newInstance();
        assertTrue(instance instanceof DomQuiescenceWatcher);
    }

    @Test
    void testInstallTrackerWithoutBrowser()
    {
        assertDoesNotThrow(() -> DomQuiescenceWatcher.installTracker());
    }

    @Test
    void testWaitForDomQuietWithoutBrowser()
    {
        assertDoesNotThrow(() -> DomQuiescenceWatcher.waitForDomQuiet());
        assertDoesNotThrow(() -> DomQuiescenceWatcher.waitForDomQuiet(Duration.ofMillis(200), Duration.ofMillis(50)));
        assertDoesNotThrow(() -> DomQuiescenceWatcher.waitForDomQuiet(null, null));
    }
}
