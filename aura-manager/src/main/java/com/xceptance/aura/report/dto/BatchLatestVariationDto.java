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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Data transfer object encapsulating an individual test variation execution in the latest batch status,
 * supporting targeted client-side filtering by environment, browser, and locale without clobbering
 * from historical catalogs.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class BatchLatestVariationDto
{
    private final String key;
    private final String variationId;
    private final String status;
    private final boolean healed;
    private final String mode;
    private final String runId;
    private final String environment;
    private final String browser;
    private final String location;
    private final String area;
    private final String batch;
    private final Map<String, String> tags;
    private final double llmCost;
    private final long llmTokens;
    private final String executionId;
    private final List<String> bugs;
    private final List<String> bugSeverities;
    private final boolean flaky;
    private final int flakyFailureCount;
    private final int flakyTotalRuns;
    private final String flakyReason;

    public BatchLatestVariationDto(
        final String key,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location)
    {
        this(key, status, healed, mode, runId, environment, browser, location, "@Browsing (default)", "Default");
    }

    public BatchLatestVariationDto(
        final String key,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area)
    {
        this(key, status, healed, mode, runId, environment, browser, location, area, "Default");
    }

    public BatchLatestVariationDto(
        final String key,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch)
    {
        this(key, status, healed, mode, runId, environment, browser, location, area, batch, Collections.emptyMap());
    }

    public BatchLatestVariationDto(
        final String key,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch,
        final Map<String, String> tags)
    {
        this(key, status, healed, mode, runId, environment, browser, location, area, batch, tags, 0.0, 0L);
    }

    public BatchLatestVariationDto(
        final String key,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch,
        final Map<String, String> tags,
        final double llmCost,
        final long llmTokens)
    {
        this(key, status, healed, mode, runId, environment, browser, location, area, batch, tags, llmCost, llmTokens, null);
    }

    public BatchLatestVariationDto(
        final String key,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch,
        final Map<String, String> tags,
        final double llmCost,
        final long llmTokens,
        final String executionId)
    {
        this(key, (key != null ? key : ""), status, healed, mode, runId, environment, browser, location, area, batch, tags, llmCost, llmTokens, executionId, Collections.emptyList());
    }

    public BatchLatestVariationDto(
        final String key,
        final String variationId,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch,
        final Map<String, String> tags,
        final double llmCost,
        final long llmTokens,
        final String executionId,
        final List<String> bugs)
    {
        this(key, variationId, status, healed, mode, runId, environment, browser, location, area, batch, tags, llmCost, llmTokens, executionId, bugs, Collections.emptyList());
    }

    public BatchLatestVariationDto(
        final String key,
        final String variationId,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch,
        final Map<String, String> tags,
        final double llmCost,
        final long llmTokens,
        final String executionId,
        final List<String> bugs,
        final List<String> bugSeverities)
    {
        this(key, variationId, status, healed, mode, runId, environment, browser, location, area, batch, tags, llmCost, llmTokens, executionId, bugs, bugSeverities, false, 0, 0, "");
    }

    public BatchLatestVariationDto(
        final String key,
        final String variationId,
        final String status,
        final boolean healed,
        final String mode,
        final String runId,
        final String environment,
        final String browser,
        final String location,
        final String area,
        final String batch,
        final Map<String, String> tags,
        final double llmCost,
        final long llmTokens,
        final String executionId,
        final List<String> bugs,
        final List<String> bugSeverities,
        final boolean flaky,
        final int flakyFailureCount,
        final int flakyTotalRuns,
        final String flakyReason)
    {
        this.key = key;
        this.variationId = (variationId != null && !variationId.isBlank()) ? variationId : (key != null ? key : "");
        this.status = status;
        this.healed = healed;
        this.mode = mode;
        this.runId = runId;
        this.environment = environment;
        this.browser = browser;
        this.location = location;
        this.area = (area != null && !area.isBlank()) ? area : "@Browsing (default)";
        this.batch = (batch != null && !batch.isBlank()) ? batch : "Default";
        this.tags = (tags != null) ? Collections.unmodifiableMap(new LinkedHashMap<>(tags)) : Collections.emptyMap();
        this.llmCost = Math.max(0.0, llmCost);
        this.llmTokens = Math.max(0L, llmTokens);
        this.executionId = executionId;
        this.bugs = (bugs != null && !bugs.isEmpty()) ? Collections.unmodifiableList(new ArrayList<>(bugs)) : Collections.emptyList();
        this.bugSeverities = (bugSeverities != null && !bugSeverities.isEmpty()) ? Collections.unmodifiableList(new ArrayList<>(bugSeverities)) : Collections.emptyList();
        this.flaky = flaky;
        this.flakyFailureCount = Math.max(0, flakyFailureCount);
        this.flakyTotalRuns = Math.max(0, flakyTotalRuns);
        this.flakyReason = (flakyReason != null) ? flakyReason : "";
    }

    public boolean isFlaky()
    {
        return flaky;
    }

    public int getFlakyFailureCount()
    {
        return flakyFailureCount;
    }

    public int getFlakyTotalRuns()
    {
        return flakyTotalRuns;
    }

    public String getFlakyReason()
    {
        return flakyReason;
    }

    public String getKey()
    {
        return key;
    }

    public String getVariationId()
    {
        return variationId;
    }

    public List<String> getBugs()
    {
        return bugs;
    }

    public boolean hasBugs()
    {
        return !bugs.isEmpty();
    }

    public List<String> getBugSeverities()
    {
        return bugSeverities;
    }

    public boolean hasBugSeverities()
    {
        return !bugSeverities.isEmpty();
    }

    public String getExecutionId()
    {
        return executionId;
    }

    public String getStatus()
    {
        return status;
    }

    public boolean isFinished()
    {
        if (status == null || status.isBlank())
        {
            return false;
        }
        final String st = status.toLowerCase(Locale.ROOT);
        return !"running".equals(st) && !"in_progress".equals(st) && !"executing".equals(st) && !"pending".equals(st);
    }

    public boolean isHealed()
    {
        return healed;
    }

    public String getMode()
    {
        return mode;
    }

    public String getRunId()
    {
        return runId;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public String getBrowser()
    {
        return browser;
    }

    public String getLocation()
    {
        return location;
    }

    public String getArea()
    {
        return area;
    }

    public String getBatch()
    {
        return batch;
    }

    public Map<String, String> getTags()
    {
        return tags;
    }

    public String getTagValue(final String tagKey)
    {
        if (tags == null || tagKey == null)
        {
            return null;
        }
        final String val = tags.get(tagKey);
        if (val != null)
        {
            return val;
        }
        for (final Map.Entry<String, String> entry : tags.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(tagKey))
            {
                return entry.getValue();
            }
        }
        return null;
    }

    public String getTagsJson()
    {
        final Map<String, String> merged = new LinkedHashMap<>();
        if (batch != null && !batch.isBlank())
        {
            merged.put("batch", batch);
        }
        if (tags != null)
        {
            for (final Map.Entry<String, String> e : tags.entrySet())
            {
                if (e.getKey() != null && e.getValue() != null)
                {
                    merged.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
                }
            }
        }
        if (merged.isEmpty())
        {
            return "{}";
        }
        final StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (final Map.Entry<String, String> entry : merged.entrySet())
        {
            if (!first)
            {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
              .append(escapeJson(entry.getValue())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public double getLlmCost()
    {
        return llmCost;
    }

    public long getLlmTokens()
    {
        return llmTokens;
    }

    private static String escapeJson(final String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
