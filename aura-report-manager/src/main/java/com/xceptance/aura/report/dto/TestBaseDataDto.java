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
import java.util.Map;

/**
 * Aggregated DTO containing dynamic filter values, areas, test classes, and metrics for the Test Base page.
 *
 * @author Xceptance GmbH 2026
 */
public final class TestBaseDataDto
{
    private final List<String> filterBatches;
    private final List<String> filterLocales;
    private final List<String> filterBrowsers;
    private final List<TestBaseAreaDto> areas;
    private final Map<String, List<String>> bugsMap;
    private final int totalTestClassesCount;
    private final int totalVariationsCount;

    public TestBaseDataDto(
        final List<String> filterBatches,
        final List<String> filterLocales,
        final List<String> filterBrowsers,
        final List<TestBaseAreaDto> areas,
        final Map<String, List<String>> bugsMap,
        final int totalTestClassesCount,
        final int totalVariationsCount)
    {
        this.filterBatches = filterBatches;
        this.filterLocales = filterLocales;
        this.filterBrowsers = filterBrowsers;
        this.areas = areas;
        this.bugsMap = bugsMap;
        this.totalTestClassesCount = totalTestClassesCount;
        this.totalVariationsCount = totalVariationsCount;
    }

    public List<String> getFilterBatches()
    {
        return filterBatches;
    }

    public List<String> getFilterLocales()
    {
        return filterLocales;
    }

    public List<String> getFilterBrowsers()
    {
        return filterBrowsers;
    }

    public List<TestBaseAreaDto> getAreas()
    {
        return areas;
    }

    public Map<String, List<String>> getBugsMap()
    {
        return bugsMap;
    }

    public int getTotalTestClassesCount()
    {
        return totalTestClassesCount;
    }

    public int getTotalVariationsCount()
    {
        return totalVariationsCount;
    }
}
