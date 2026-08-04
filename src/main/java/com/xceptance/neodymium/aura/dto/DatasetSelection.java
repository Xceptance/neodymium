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
package com.xceptance.neodymium.aura.dto;

/**
 * Data transfer object representing a selected dataset inside a YAML test file.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class DatasetSelection
{
    public String file;
    public String id; // testId or index

    public DatasetSelection()
    {
    }

    public DatasetSelection(final String file, final String id)
    {
        this.file = file;
        this.id = id;
    }
}
