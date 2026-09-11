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
package com.xceptance.aura.report.dto;

/**
 * Data transfer object representing a detected Java test class inside src/test/java.
 *
 * @author Xceptance GmbH 2026
 */
public final class TestClassInfoDto
{
    private final String simpleName;
    private final String fullyQualifiedName;
    private final String packageName;
    private final String category;

    public TestClassInfoDto(
        final String simpleName,
        final String fullyQualifiedName,
        final String packageName,
        final String category)
    {
        this.simpleName = simpleName;
        this.fullyQualifiedName = fullyQualifiedName;
        this.packageName = packageName;
        this.category = category;
    }

    public String getSimpleName()
    {
        return simpleName;
    }

    public String getFullyQualifiedName()
    {
        return fullyQualifiedName;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public String getCategory()
    {
        return category;
    }
}
