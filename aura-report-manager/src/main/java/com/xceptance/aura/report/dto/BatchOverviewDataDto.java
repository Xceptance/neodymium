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

import com.xceptance.aura.report.entity.TestRunEntity;
import java.util.List;

/**
 * DTO containing dynamic batch cards, run records, and dynamic filter values for the Batch Overview page.
 *
 * @author Xceptance GmbH 2026
 */
public final class BatchOverviewDataDto
{
    private final List<BatchSummaryDto> batches;
    private final List<TestRunEntity> runs;
    private final List<String> filterEnvs;
    private final List<String> filterLocales;
    private final List<String> filterBrowsers;

    public BatchOverviewDataDto(
        final List<BatchSummaryDto> batches,
        final List<TestRunEntity> runs,
        final List<String> filterEnvs,
        final List<String> filterLocales,
        final List<String> filterBrowsers)
    {
        this.batches = batches;
        this.runs = runs;
        this.filterEnvs = filterEnvs;
        this.filterLocales = filterLocales;
        this.filterBrowsers = filterBrowsers;
    }

    public List<BatchSummaryDto> getBatches()
    {
        return batches;
    }

    public List<TestRunEntity> getRuns()
    {
        return runs;
    }

    public List<String> getFilterEnvs()
    {
        return filterEnvs;
    }

    public List<String> getFilterLocales()
    {
        return filterLocales;
    }

    public List<String> getFilterBrowsers()
    {
        return filterBrowsers;
    }
}
