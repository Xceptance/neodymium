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
    private final int passHealedCount;
    private final int fixedHealedCount;
    private final int knownHealedCount;
    private final int unknownHealedCount;
    private final int passAiCount;
    private final int fixedAiCount;
    private final int knownAiCount;
    private final int unknownAiCount;

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
        this(runId, timestampLabel, passCount, fixedCount, knownCount, unknownCount, ignoredCount, totalCount, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public AreaRunPointDto(
        final String runId,
        final String timestampLabel,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int totalCount,
        final int passHealedCount,
        final int fixedHealedCount,
        final int knownHealedCount,
        final int unknownHealedCount)
    {
        this(runId, timestampLabel, passCount, fixedCount, knownCount, unknownCount, ignoredCount, totalCount,
             passHealedCount, fixedHealedCount, knownHealedCount, unknownHealedCount, 0, 0, 0, 0);
    }

    public AreaRunPointDto(
        final String runId,
        final String timestampLabel,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int totalCount,
        final int passHealedCount,
        final int fixedHealedCount,
        final int knownHealedCount,
        final int unknownHealedCount,
        final int passAiCount,
        final int fixedAiCount,
        final int knownAiCount,
        final int unknownAiCount)
    {
        this.runId = runId;
        this.timestampLabel = timestampLabel;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.totalCount = totalCount;
        this.passHealedCount = passHealedCount;
        this.fixedHealedCount = fixedHealedCount;
        this.knownHealedCount = knownHealedCount;
        this.unknownHealedCount = unknownHealedCount;
        this.passAiCount = passAiCount;
        this.fixedAiCount = fixedAiCount;
        this.knownAiCount = knownAiCount;
        this.unknownAiCount = unknownAiCount;
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

    public int getPassHealedCount()
    {
        return passHealedCount;
    }

    public int getFixedHealedCount()
    {
        return fixedHealedCount;
    }

    public int getKnownHealedCount()
    {
        return knownHealedCount;
    }

    public int getUnknownHealedCount()
    {
        return unknownHealedCount;
    }

    public int getPassAiCount()
    {
        return passAiCount;
    }

    public int getFixedAiCount()
    {
        return fixedAiCount;
    }

    public int getKnownAiCount()
    {
        return knownAiCount;
    }

    public int getUnknownAiCount()
    {
        return unknownAiCount;
    }

    public int getTotalAiCount()
    {
        return passAiCount + fixedAiCount + knownAiCount + unknownAiCount;
    }

    public int getPassCleanCount()
    {
        return Math.max(0, passCount - passHealedCount - passAiCount);
    }

    public int getFixedCleanCount()
    {
        return Math.max(0, fixedCount - fixedHealedCount - fixedAiCount);
    }

    public int getKnownCleanCount()
    {
        return Math.max(0, knownCount - knownHealedCount - knownAiCount);
    }

    public int getUnknownCleanCount()
    {
        return Math.max(0, unknownCount - unknownHealedCount - unknownAiCount);
    }
}
