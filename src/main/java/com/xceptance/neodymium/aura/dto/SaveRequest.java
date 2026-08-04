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
 * Data transfer object representing a request to save a YAML file.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class SaveRequest
{
    public String file;
    public String content;

    public SaveRequest()
    {
    }

    public SaveRequest(final String file, final String content)
    {
        this.file = file;
        this.content = content;
    }
}
