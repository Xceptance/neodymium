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
 * Data transfer object representing execution statistics for a test area in a single test run.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class AreaRunPointDto
{
    private final String runId;
    private final String timestampLabel;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final int totalCount;

    public AreaRunPointDto(
        final String runId,
        final String timestampLabel,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int totalCount)
    {
        this.runId = runId;
        this.timestampLabel = timestampLabel;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.totalCount = totalCount;
    }

    public String getRunId()
    {
        return runId;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public int getPassCount()
    {
        return passCount;
    }

    public int getFixedCount()
    {
        return fixedCount;
    }

    public int getKnownCount()
    {
        return knownCount;
    }

    public int getUnknownCount()
    {
        return unknownCount;
    }

    public int getIgnoredCount()
    {
        return ignoredCount;
    }

    public int getTotalCount()
    {
        return totalCount;
    }
}
