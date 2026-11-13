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

import java.util.List;

/**
 * DTO representing a sub-area group, class count, and grouped test classes in Test Base.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class TestBaseSubAreaDto
{
    private final String subAreaName;
    private final String subAreaPaneId;
    private final int classCount;
    private final List<TestBaseClassDto> testClasses;

    public TestBaseSubAreaDto(
        final String subAreaName,
        final String subAreaPaneId,
        final int classCount,
        final List<TestBaseClassDto> testClasses)
    {
        this.subAreaName = subAreaName;
        this.subAreaPaneId = subAreaPaneId;
        this.classCount = classCount;
        this.testClasses = testClasses;
    }

    public String getSubAreaName()
    {
        return subAreaName;
    }

    public String getSubAreaPaneId()
    {
        return subAreaPaneId;
    }

    public int getClassCount()
    {
        return classCount;
    }

    public List<TestBaseClassDto> getTestClasses()
    {
        return testClasses;
    }

    public int getTotalVariationsCount()
    {
        if (testClasses == null)
        {
            return 0;
        }
        return testClasses.stream()
            .mapToInt(TestBaseClassDto::getVariationCount)
            .sum();
    }
}
