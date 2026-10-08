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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DTO representing a historical execution of a test variation across test runs.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class TestBaseVariationHistoryDto
{
    private static final DateTimeFormatter STANDARD_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter RUN_ID_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern RUN_ID_TIMESTAMP_PATTERN = Pattern.compile("(\\d{8})_(\\d{6})");

    /**
     * Comparator for sorting historical variation test executions by timestamp in descending order (newest first).
     * If timestamps are equal or identical, falls back to secondary sorting by runId descending.
     */
    public static final Comparator<TestBaseVariationHistoryDto> BY_TIMESTAMP_DESC = (final TestBaseVariationHistoryDto a, final TestBaseVariationHistoryDto b) -> {
        final long aTime = parseTimestampToEpoch(a != null ? a.getTimestamp() : null, a != null ? a.getRunId() : null);
        final long bTime = parseTimestampToEpoch(b != null ? b.getTimestamp() : null, b != null ? b.getRunId() : null);
        final int cmp = Long.compare(bTime, aTime);
        if (cmp != 0)
        {
            return cmp;
        }
        final String aRun = a != null && a.getRunId() != null ? a.getRunId() : "";
        final String bRun = b != null && b.getRunId() != null ? b.getRunId() : "";
        return bRun.compareToIgnoreCase(aRun);
    };
    private final String runId;
    private final String executionId;
    private final String batchName;
    private final String mode;
    private final String engine;
    private final String timestamp;
    private final String status;
    private final String statusClass;
    private final String statusLabel;
    private final List<String> bugs;
    private final boolean healed;
    private final boolean passedHealed;
    private final boolean failedHealed;

    public TestBaseVariationHistoryDto(
        final String runId,
        final String executionId,
        final String batchName,
        final String mode,
        final String engine,
        final String timestamp,
        final String status,
        final String statusClass,
        final String statusLabel,
        final List<String> bugs,
        final boolean healed,
        final boolean passedHealed,
        final boolean failedHealed)
    {
        this.runId = runId != null ? runId : "";
        this.executionId = executionId != null ? executionId : "";
        this.batchName = batchName != null ? batchName : "Unknown";
        this.mode = mode != null && !mode.isBlank() ? mode : "FORCE_RECORDING";
        this.engine = engine != null ? engine : "Java";
        this.timestamp = formatTimestamp(timestamp);
        this.status = status != null ? status : "passed-clean";
        this.statusClass = statusClass != null ? statusClass : "badge-pass";
        this.statusLabel = statusLabel != null ? statusLabel : "PASSED";
        this.bugs = bugs != null ? new ArrayList<>(bugs) : new ArrayList<>();
        this.healed = healed;
        this.passedHealed = passedHealed;
        this.failedHealed = failedHealed;
    }

    public TestBaseVariationHistoryDto(
        final String runId,
        final String executionId,
        final String batchName,
        final String mode,
        final String engine,
        final String timestamp,
        final String status,
        final String statusClass,
        final String statusLabel,
        final List<String> bugs)
    {
        this(runId, executionId, batchName, mode, engine, timestamp, status, statusClass, statusLabel, bugs, false, false, false);
    }

    private static String formatTimestamp(final String raw)
    {
        if (raw == null || raw.trim().isEmpty() || "Recently".equalsIgnoreCase(raw.trim()))
        {
            return "Recently";
        }
        final String trimmed = raw.trim();
        try
        {
            final Instant instant = Instant.parse(trimmed);
            final LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return ldt.format(STANDARD_DATE_TIME_FORMATTER);
        }
        catch (final Exception e1)
        {
            try
            {
                final LocalDateTime ldt = LocalDateTime.parse(trimmed.replace(" ", "T"));
                return ldt.format(STANDARD_DATE_TIME_FORMATTER);
            }
            catch (final Exception e2)
            {
                return trimmed;
            }
        }
    }

    /**
     * Parses a raw or formatted timestamp into epoch milliseconds for robust chronological sorting.
     * Falls back to runId extraction if timestamp is missing or "Recently".
     *
     * @param raw the timestamp string
     * @param runId the run identifier
     * @return epoch milliseconds
     */
    public static long parseTimestampToEpoch(final String raw, final String runId)
    {
        if (raw == null || raw.trim().isEmpty() || "Recently".equalsIgnoreCase(raw.trim()))
        {
            final long fromRunId = extractEpochFromRunId(runId);
            if (fromRunId > 0L)
            {
                return fromRunId;
            }
            return Long.MAX_VALUE;
        }
        final String trimmed = raw.trim();
        try
        {
            final LocalDateTime ldt = LocalDateTime.parse(trimmed, STANDARD_DATE_TIME_FORMATTER);
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        catch (final Exception e1)
        {
            try
            {
                final Instant instant = Instant.parse(trimmed);
                return instant.toEpochMilli();
            }
            catch (final Exception e2)
            {
                try
                {
                    final LocalDateTime ldt = LocalDateTime.parse(trimmed.replace(" ", "T"));
                    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
                catch (final Exception e3)
                {
                    final long fromRunId = extractEpochFromRunId(runId);
                    return fromRunId > 0L ? fromRunId : 0L;
                }
            }
        }
    }

    private static long extractEpochFromRunId(final String runId)
    {
        if (runId == null || runId.isBlank())
        {
            return 0L;
        }
        final Matcher matcher = RUN_ID_TIMESTAMP_PATTERN.matcher(runId);
        if (matcher.find())
        {
            final String datePart = matcher.group(1);
            final String timePart = matcher.group(2);
            try
            {
                final LocalDateTime ldt = LocalDateTime.parse(datePart + timePart, RUN_ID_DATE_TIME_FORMATTER);
                return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            catch (final Exception e)
            {
                return 0L;
            }
        }
        return 0L;
    }

    public TestBaseVariationHistoryDto(
        final String runId,
        final String batchName,
        final String mode,
        final String engine,
        final String timestamp,
        final String status,
        final String statusClass,
        final String statusLabel,
        final List<String> bugs)
    {
        this(runId, "", batchName, mode, engine, timestamp, status, statusClass, statusLabel, bugs, false, false, false);
    }

    public String getRunId()
    {
        return runId;
    }

    public String getExecutionId()
    {
        return executionId;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public String getMode()
    {
        return mode;
    }

    public String getEngine()
    {
        return engine;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public String getStatus()
    {
        return status;
    }

    public String getStatusClass()
    {
        return statusClass;
    }

    public String getStatusLabel()
    {
        return statusLabel;
    }

    public List<String> getBugs()
    {
        return bugs;
    }

    public boolean isHealed()
    {
        return healed;
    }

    public boolean isPassedHealed()
    {
        return passedHealed;
    }

    public boolean isFailedHealed()
    {
        return failedHealed;
    }
}
