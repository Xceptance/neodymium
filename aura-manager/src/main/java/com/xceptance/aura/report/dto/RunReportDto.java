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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data transfer object representing a single run report.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class RunReportDto
{
    private final String runId;
    private final String batchName;
    private final String timestamp;
    private final String duration;
    private final int totalCount;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final int runningCount;
    private final List<TestExecutionDto> executions;
    private final List<AreaSummaryDto> areaSummaries;
    private final int totalLlmCalls;
    private final long totalLlmTokens;
    private final double totalLlmCost;
    private final Map<String, String> tags;

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int runningCount,
        final List<TestExecutionDto> executions,
        final List<AreaSummaryDto> areaSummaries,
        final int totalLlmCalls,
        final long totalLlmTokens,
        final double totalLlmCost,
        final Map<String, String> tags)
    {
        this.runId = runId;
        this.batchName = batchName;
        this.timestamp = timestamp;
        this.duration = duration;
        this.totalCount = totalCount;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.runningCount = runningCount;
        this.executions = executions != null ? executions : new ArrayList<>();
        this.areaSummaries = areaSummaries != null ? areaSummaries : new ArrayList<>();
        this.totalLlmCalls = totalLlmCalls;
        this.totalLlmTokens = totalLlmTokens;
        this.totalLlmCost = totalLlmCost;
        this.tags = tags != null ? new LinkedHashMap<>(tags) : new LinkedHashMap<>();
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int runningCount,
        final List<TestExecutionDto> executions,
        final List<AreaSummaryDto> areaSummaries,
        final int totalLlmCalls,
        final long totalLlmTokens,
        final double totalLlmCost)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, runningCount, executions, areaSummaries, totalLlmCalls, totalLlmTokens, totalLlmCost, Map.of());
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions,
        final List<AreaSummaryDto> areaSummaries,
        final int totalLlmCalls,
        final long totalLlmTokens,
        final double totalLlmCost)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, 0, executions, areaSummaries, totalLlmCalls, totalLlmTokens, totalLlmCost);
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int runningCount,
        final List<TestExecutionDto> executions,
        final List<AreaSummaryDto> areaSummaries)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, runningCount, executions, areaSummaries, 0, 0L, 0.0);
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions,
        final List<AreaSummaryDto> areaSummaries)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, 0, executions, areaSummaries, 0, 0L, 0.0);
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int runningCount,
        final List<TestExecutionDto> executions)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, runningCount, executions, new ArrayList<>());
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, 0, executions, new ArrayList<>());
    }

    public String getRunId()
    {
        return runId;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public Map<String, String> getTags()
    {
        return tags != null ? tags : Map.of();
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public String getDuration()
    {
        return duration;
    }

    public int getTotalCount()
    {
        return totalCount;
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

    public int getRunningCount()
    {
        return runningCount;
    }

    public List<TestExecutionDto> getExecutions()
    {
        return executions;
    }

    public List<AreaSummaryDto> getAreaSummaries()
    {
        return areaSummaries;
    }

    public List<String> getFilterLocations()
    {
        if (executions == null || executions.isEmpty())
        {
            return List.of();
        }
        return executions.stream()
            .map(e -> (e.getLocation() == null || e.getLocation().isBlank()) ? "Default" : e.getLocation().trim())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public List<String> getFilterLocales()
    {
        return getFilterLocations();
    }

    public List<String> getFilterBrowsers()
    {
        if (executions == null || executions.isEmpty())
        {
            return List.of();
        }
        return executions.stream()
            .map(e -> (e.getBrowser() == null || e.getBrowser().isBlank()) ? "Unknown" : e.getBrowser().trim())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public int getHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            final int stepsHealed = executions.stream().mapToInt(TestExecutionDto::getHealedStepsCount).sum();
            if (stepsHealed > 0)
            {
                return stepsHealed;
            }
        }
        return 0;
    }

    public int getPassedHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isPassedHealed).count();
        }
        return 0;
    }

    public int getFailedHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isFailedHealed).count();
        }
        return 0;
    }

    public int getPassHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isPassHealed).count();
        }
        return 0;
    }

    public int getFixedHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isFixedHealed).count();
        }
        return 0;
    }

    public int getKnownHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isKnownHealed).count();
        }
        return 0;
    }

    public int getUnknownHealedCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isUnknownHealed).count();
        }
        return 0;
    }

    public int getTotalHealedCount()
    {
        return getPassHealedCount() + getFixedHealedCount() + getKnownHealedCount() + getUnknownHealedCount();
    }

    public int getPassAiCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isPassAi).count();
        }
        return 0;
    }

    public int getFixedAiCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isFixedAi).count();
        }
        return 0;
    }

    public int getKnownAiCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isKnownAi).count();
        }
        return 0;
    }

    public int getUnknownAiCount()
    {
        if (executions != null && !executions.isEmpty())
        {
            return (int) executions.stream().filter(TestExecutionDto::isUnknownAi).count();
        }
        return 0;
    }

    public int getTotalAiCount()
    {
        return getPassAiCount() + getFixedAiCount() + getKnownAiCount() + getUnknownAiCount();
    }

    public int getFailCount()
    {
        return knownCount + unknownCount;
    }

    public long getTotalDurationMs()
    {
        if (executions == null || executions.isEmpty())
        {
            return 0L;
        }

        long minStartMs = Long.MAX_VALUE;
        long maxStartMs = Long.MIN_VALUE;
        TestExecutionDto latestExec = null;
        long sumDurationMs = 0L;
        int validTimestampCount = 0;

        for (final TestExecutionDto exec : executions)
        {
            sumDurationMs += exec.getDurationMs();
            final long startMs = exec.getTimestampMs();
            if (startMs > 0L)
            {
                validTimestampCount++;
                if (startMs < minStartMs)
                {
                    minStartMs = startMs;
                }
                if (startMs >= maxStartMs)
                {
                    maxStartMs = startMs;
                    latestExec = exec;
                }
            }
        }

        if (latestExec == null || validTimestampCount < 1)
        {
            return sumDurationMs;
        }

        if (validTimestampCount < 2 && executions.size() > 1)
        {
            return sumDurationMs;
        }

        final long wallClockDurationMs = (maxStartMs - minStartMs) + latestExec.getDurationMs();
        return wallClockDurationMs > 0L ? wallClockDurationMs : sumDurationMs;
    }

    public String getTotalDurationFormatted()
    {
        if (duration != null && !duration.isBlank() && !"0s".equalsIgnoreCase(duration.trim()) && !"0 ms".equalsIgnoreCase(duration.trim()))
        {
            return duration;
        }
        final long totalMs = getTotalDurationMs();
        if (totalMs <= 0)
        {
            return "0 min 0 s";
        }
        final long totalSeconds = Math.round(totalMs / 1000.0);
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return minutes + " min " + seconds + " s";
    }

    public double getTotalLlmCost()
    {
        if (totalLlmCost > 0.0)
        {
            return totalLlmCost;
        }
        if (executions == null || executions.isEmpty())
        {
            return 0.0;
        }
        return executions.stream().mapToDouble(TestExecutionDto::getLlmCost).sum();
    }

    public String getTotalLlmCostFormatted()
    {
        final double roundedUp = Math.ceil(getTotalLlmCost() * 10000.0) / 10000.0;
        return String.format("$%.4f", roundedUp);
    }

    public int getTotalLlmCalls()
    {
        if (totalLlmCalls > 0)
        {
            return totalLlmCalls;
        }
        if (executions == null || executions.isEmpty())
        {
            return 0;
        }
        return executions.stream().mapToInt(TestExecutionDto::getLlmCallsCount).sum();
    }

    public long getTotalLlmTokens()
    {
        if (totalLlmTokens > 0L)
        {
            return totalLlmTokens;
        }
        if (executions == null || executions.isEmpty())
        {
            return 0L;
        }
        return executions.stream().mapToLong(TestExecutionDto::getLlmTotalTokens).sum();
    }

    public String getTotalLlmTokensFormatted()
    {
        return String.format("%,d", getTotalLlmTokens());
    }

    public List<String> getFilterModes()
    {
        if (executions == null || executions.isEmpty())
        {
            return List.of();
        }
        return executions.stream()
            .map(TestExecutionDto::getDisplayMode)
            .filter(m -> m != null && !m.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public int getPassCleanCount()
    {
        return Math.max(0, passCount - getPassHealedCount() - getPassAiCount());
    }

    public int getFixedCleanCount()
    {
        return Math.max(0, fixedCount - getFixedHealedCount() - getFixedAiCount());
    }

    public int getKnownCleanCount()
    {
        return Math.max(0, knownCount - getKnownHealedCount() - getKnownAiCount());
    }

    public int getUnknownCleanCount()
    {
        return Math.max(0, unknownCount - getUnknownHealedCount() - getUnknownAiCount());
    }

    public double getPassedPct()
    {
        return totalCount > 0 ? (passCount * 100.0 / totalCount) : 0.0;
    }

    public double getPassCleanPct()
    {
        return totalCount > 0 ? (getPassCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getPassHealedPct()
    {
        return totalCount > 0 ? (getPassHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getPassAiPct()
    {
        return totalCount > 0 ? (getPassAiCount() * 100.0 / totalCount) : 0.0;
    }

    public double getFixedPct()
    {
        return totalCount > 0 ? (fixedCount * 100.0 / totalCount) : 0.0;
    }

    public double getFixedCleanPct()
    {
        return totalCount > 0 ? (getFixedCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getFixedHealedPct()
    {
        return totalCount > 0 ? (getFixedHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getFixedAiPct()
    {
        return totalCount > 0 ? (getFixedAiCount() * 100.0 / totalCount) : 0.0;
    }

    public double getKnownPct()
    {
        return totalCount > 0 ? (knownCount * 100.0 / totalCount) : 0.0;
    }

    public double getKnownCleanPct()
    {
        return totalCount > 0 ? (getKnownCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getKnownHealedPct()
    {
        return totalCount > 0 ? (getKnownHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getKnownAiPct()
    {
        return totalCount > 0 ? (getKnownAiCount() * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownPct()
    {
        return totalCount > 0 ? (unknownCount * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownCleanPct()
    {
        return totalCount > 0 ? (getUnknownCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownHealedPct()
    {
        return totalCount > 0 ? (getUnknownHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownAiPct()
    {
        return totalCount > 0 ? (getUnknownAiCount() * 100.0 / totalCount) : 0.0;
    }

    public double getIgnoredPct()
    {
        return totalCount > 0 ? (ignoredCount * 100.0 / totalCount) : 0.0;
    }

    public String getPassRateInt()
    {
        if (totalCount <= 0)
        {
            return "0%";
        }
        final int numPassed = passCount + fixedCount;
        final double rate = (numPassed * 100.0) / totalCount;
        return String.format(Locale.US, "%.0f%%", rate);
    }

    public String getPassRateText()
    {
        if (totalCount <= 0)
        {
            return "N/A";
        }
        final int numPassed = passCount + fixedCount;
        final double rate = (numPassed * 100.0) / totalCount;
        return String.format(Locale.US, "%.0f%% Pass", rate);
    }

    public String getPassCleanDashArray()
    {
        final double p = getPassCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassCleanDashOffset()
    {
        return "25";
    }

    public String getPassHealedDashArray()
    {
        final double p = getPassHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassCleanPct());
    }

    public String getPassAiDashArray()
    {
        final double p = getPassAiPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassAiDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassCleanPct() - getPassHealedPct());
    }

    public String getFixedCleanDashArray()
    {
        final double p = getFixedCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedCleanDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct());
    }

    public String getFixedHealedDashArray()
    {
        final double p = getFixedHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedCleanPct());
    }

    public String getFixedAiDashArray()
    {
        final double p = getFixedAiPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedAiDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedCleanPct() - getFixedHealedPct());
    }

    public String getKnownCleanDashArray()
    {
        final double p = getKnownCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownCleanDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct());
    }

    public String getKnownHealedDashArray()
    {
        final double p = getKnownHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownCleanPct());
    }

    public String getKnownAiDashArray()
    {
        final double p = getKnownAiPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownAiDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownCleanPct() - getKnownHealedPct());
    }

    public String getUnknownCleanDashArray()
    {
        final double p = getUnknownCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownCleanDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct());
    }

    public String getUnknownHealedDashArray()
    {
        final double p = getUnknownHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct() - getUnknownCleanPct());
    }

    public String getUnknownAiDashArray()
    {
        final double p = getUnknownAiPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownAiDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct() - getUnknownCleanPct() - getUnknownHealedPct());
    }

    public String getIgnoredDashArray()
    {
        final double p = getIgnoredPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getIgnoredDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct() - getUnknownPct());
    }

    public List<DistributionRatioDto> getBrowserRatios()
    {
        final List<DistributionRatioDto> browserRatios = new ArrayList<>();
        if (executions == null || executions.isEmpty())
        {
            return browserRatios;
        }

        final Map<String, Integer> bMap = new LinkedHashMap<>();
        for (final TestExecutionDto exec : executions)
        {
            final String b = (exec.getBrowser() != null && !exec.getBrowser().isBlank()) ? exec.getBrowser().trim() : "Default";
            bMap.put(b, bMap.getOrDefault(b, 0) + 1);
        }

        final String[] palette = new String[] {
            "#10b981", "#3b82f6", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4", "#6366f1", "#14b8a6"
        };

        final int bTotal = bMap.values().stream().mapToInt(Integer::intValue).sum();
        int colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : bMap.entrySet())
        {
            final double pct = bTotal > 0 ? ((double) entry.getValue() / bTotal * 100.0) : 0.0;
            browserRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }
        return browserRatios;
    }

    public List<DistributionRatioDto> getLocaleRatios()
    {
        final List<DistributionRatioDto> localeRatios = new ArrayList<>();
        if (executions == null || executions.isEmpty())
        {
            return localeRatios;
        }

        final Map<String, Integer> lMap = new LinkedHashMap<>();
        for (final TestExecutionDto exec : executions)
        {
            final String loc = (exec.getLocation() != null && !exec.getLocation().isBlank() && !exec.getLocation().equalsIgnoreCase("UNKNOWN"))
                ? exec.getLocation().trim()
                : "Default";
            lMap.put(loc, lMap.getOrDefault(loc, 0) + 1);
        }

        final String[] palette = new String[] {
            "#10b981", "#3b82f6", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4", "#6366f1", "#14b8a6"
        };

        final int lTotal = lMap.values().stream().mapToInt(Integer::intValue).sum();
        int colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : lMap.entrySet())
        {
            final double pct = lTotal > 0 ? ((double) entry.getValue() / lTotal * 100.0) : 0.0;
            localeRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }
        return localeRatios;
    }
}
