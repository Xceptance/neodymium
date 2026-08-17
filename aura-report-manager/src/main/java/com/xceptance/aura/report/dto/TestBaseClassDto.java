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

import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import java.util.List;

/**
 * DTO representing a grouped test class and its variation executions in Test Base.
 *
 * @author Xceptance GmbH 2026
 */
public final class TestBaseClassDto
{
    private final String className;
    private final String classContainerId;
    private final int variationCount;
    private final List<TestBaseVariationEntity> variations;

    public TestBaseClassDto(
        final String className,
        final String classContainerId,
        final int variationCount,
        final List<TestBaseVariationEntity> variations)
    {
        this.className = className;
        this.classContainerId = classContainerId;
        this.variationCount = variationCount;
        this.variations = variations;
    }

    public String getClassName()
    {
        return className;
    }

    public String getClassContainerId()
    {
        return classContainerId;
    }

    public int getVariationCount()
    {
        return variationCount;
    }

    public List<TestBaseVariationEntity> getVariations()
    {
        return variations;
    }
}
