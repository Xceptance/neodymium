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
package com.xceptance.aura.test.util;

/**
 * Utility helper methods for Aura Test Manager.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraTestUtils
{
    private AuraTestUtils()
    {
    }

    public static String stripAnsi(final String input)
    {
        if (input == null)
        {
            return "";
        }
        return input.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }
}
