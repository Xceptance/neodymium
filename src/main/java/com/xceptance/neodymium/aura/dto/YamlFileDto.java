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

import java.util.List;

/**
 * Data transfer object representing a scanned YAML test file and its associated datasets.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class YamlFileDto
{
    public final String file;
    public final List<DatasetDto> datasets;

    public YamlFileDto(final String file, final List<DatasetDto> datasets)
    {
        this.file = file;
        this.datasets = datasets;
    }

    public String getFileName()
    {
        if (file == null)
        {
            return "";
        }
        final int lastSlash = file.lastIndexOf('/');
        return lastSlash >= 0 ? file.substring(lastSlash + 1) : file;
    }

    public String getFileNameWithoutExtension()
    {
        final String name = getFileName();
        final int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }
}
