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
import java.util.List;

/**
 * DTO representing a historical execution of a test variation across test runs.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class TestBaseVariationHistoryDto
{
    private final String runId;
    private final String executionId;
    private final String batchName;
    private final String mode;
    private final String timestamp;
    private final String status;
    private final String statusClass;
    private final String statusLabel;
    private final List<String> bugs;

    public TestBaseVariationHistoryDto(
        final String runId,
        final String executionId,
        final String batchName,
        final String mode,
        final String timestamp,
        final String status,
        final String statusClass,
        final String statusLabel,
        final List<String> bugs)
    {
        this.runId = runId != null ? runId : "";
        this.executionId = executionId != null ? executionId : "";
        this.batchName = batchName != null ? batchName : "Unknown";
        this.mode = mode != null && !mode.isBlank() ? mode : "FORCE_RECORDING";
        this.timestamp = formatTimestamp(timestamp);
        this.status = status != null ? status : "passed-clean";
        this.statusClass = statusClass != null ? statusClass : "badge-pass";
        this.statusLabel = statusLabel != null ? statusLabel : "PASSED";
        this.bugs = bugs != null ? new ArrayList<>(bugs) : new ArrayList<>();
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
            final java.time.Instant instant = java.time.Instant.parse(trimmed);
            final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        catch (final Exception e1)
        {
            try
            {
                final java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(trimmed.replace(" ", "T"));
                return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            catch (final Exception e2)
            {
                return trimmed;
            }
        }
    }

    public TestBaseVariationHistoryDto(
        final String runId,
        final String batchName,
        final String mode,
        final String timestamp,
        final String status,
        final String statusClass,
        final String statusLabel,
        final List<String> bugs)
    {
        this(runId, "", batchName, mode, timestamp, status, statusClass, statusLabel, bugs);
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
}
