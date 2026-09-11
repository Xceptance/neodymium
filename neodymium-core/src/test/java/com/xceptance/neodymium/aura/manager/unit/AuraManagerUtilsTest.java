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
package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests utility and helper methods of the Aura Manager.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraManagerUtilsTest
{
    @Test
    public void testStripAnsi()
    {
        Assertions.assertEquals("[INFO] Scanning for projects...", NeodymiumAuraManager.stripAnsi("[INFO] Scanning for projects..."));
        Assertions.assertEquals("[WARNING] Deprecated method...", NeodymiumAuraManager.stripAnsi("[WARNING] Deprecated method..."));
        Assertions.assertEquals("[ERROR] compilation error", NeodymiumAuraManager.stripAnsi("[ERROR] compilation error"));
        Assertions.assertEquals("INFO Scanning for projects...", NeodymiumAuraManager.stripAnsi("\u001B[1;34mINFO\u001B[m Scanning for projects..."));
        Assertions.assertEquals("[ INFO ] Scanning for projects...", NeodymiumAuraManager.stripAnsi("[ [1;34mINFO [m] Scanning for projects..."));

        Assertions.assertEquals("[INFO] Scanning for projects...", AuraQueueService.stripAnsi("[INFO] Scanning for projects..."));
    }
}
