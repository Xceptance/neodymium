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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object representing a single test variation execution.
 * Aligned with console-execution-1.json schema while preserving bugs, comments, failure, etc.
 *
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TestExecutionDto
{
    private final String id;
    private final String runId;
    private final String testClass;
    private final String title;
    private final String testName;
    private final String playbookFile;
    private final String testFile;
    private String status;
    private final String engine;
    private final String location;
    private final String browser;
    private final String failure;
    private final List<String> bugs;
    private final String comment;
    private final String areaName;
    private final List<String> junitTags;
    private final Map<String, String> dataBindings;
    private final Map<String, String> localDataBindings;
    private final JsonNode blocks;
    private final JsonNode steps;
    private final String executionMode;
    private final String startTime;
    private final String dateFormatted;
    private final String timeFormatted;
    private final long timestampMs;
    private final int totalStepsCount;
    private final int failedStepsCount;
    private final long durationMs;
    private final String durationFormatted;
    private final int llmCallsCount;
    private final long llmTotalTokens;
    private final double llmCost;
    private final String failureSnippet;

    public TestExecutionDto()
    {
        this("", "", "", "", "", "", "", "passed-clean", "Java", "UNKNOWN", null, "Unknown", "NONE", new ArrayList<>(), null, "Browsing (default)", new ArrayList<>(), new HashMap<>(), new HashMap<>(), null, null, null, null, "FORCE_RECORDING", null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null);
    }

    @JsonCreator
    public TestExecutionDto(
        @JsonProperty("id") final String id,
        @JsonProperty("runId") final String runId,
        @JsonProperty("testClass") final String testClass,
        @JsonProperty("title") final String title,
        @JsonProperty("testName") final String testName,
        @JsonProperty("playbookFile") final String playbookFile,
        @JsonProperty("testFile") final String testFile,
        @JsonProperty("status") final String status,
        @JsonProperty("engine") final String engine,
        @JsonProperty("location") final String location,
        @JsonProperty("locale") final String locale,
        @JsonProperty("browser") final String browser,
        @JsonProperty("failure") final String failure,
        @JsonProperty("bugs") final List<String> bugs,
        @JsonProperty("comment") final String comment,
        @JsonProperty("areaName") final String areaName,
        @JsonProperty("junitTags") final List<String> junitTags,
        @JsonProperty("dataBindings") final Map<String, String> dataBindings,
        @JsonProperty("localDataBindings") final Map<String, String> localDataBindings,
        @JsonProperty("blocks") final JsonNode blocks,
        @JsonProperty("steps") final JsonNode steps,
        @JsonProperty("testId") final String testId,
        @JsonProperty("datasetId") final String datasetId,
        @JsonProperty("executionMode") final String executionMode,
        @JsonProperty("mode") final String mode,
        @JsonProperty("startTime") final String startTime,
        @JsonProperty("dateFormatted") final String dateFormatted,
        @JsonProperty("timeFormatted") final String timeFormatted,
        @JsonProperty("timestampMs") final Long timestampMs,
        @JsonProperty("totalStepsCount") final Integer totalStepsCount,
        @JsonProperty("failedStepsCount") final Integer failedStepsCount,
        @JsonProperty("durationMs") final Long durationMs,
        @JsonProperty("durationFormatted") final String durationFormatted,
        @JsonProperty("llmCallsCount") final Integer llmCallsCount,
        @JsonProperty("llmTotalTokens") final Long llmTotalTokens,
        @JsonProperty("llmCost") final Double llmCost,
        @JsonProperty("failureSnippet") final String failureSnippet)
    {
        final String effectiveTestClass;
        if (testClass != null && !testClass.trim().isEmpty())
        {
            effectiveTestClass = testClass.trim();
        }
        else if (testFile != null && !testFile.trim().isEmpty())
        {
            String tf = testFile.trim();
            if (tf.contains("#"))
            {
                tf = tf.substring(0, tf.indexOf('#'));
            }
            if (tf.contains("."))
            {
                tf = tf.substring(tf.lastIndexOf('.') + 1);
            }
            effectiveTestClass = !tf.trim().isEmpty() ? tf.trim() : "DefaultClass";
        }
        else if (junitTags != null && !junitTags.isEmpty() && !junitTags.get(0).trim().isEmpty())
        {
            effectiveTestClass = junitTags.get(0).trim();
        }
        else
        {
            effectiveTestClass = "DefaultClass";
        }
        this.testClass = effectiveTestClass;

        if (id != null && !id.trim().isEmpty())
        {
            this.id = id.trim();
        }
        else if (testId != null && !testId.trim().isEmpty())
        {
            this.id = testId.trim();
        }
        else if (datasetId != null && !datasetId.trim().isEmpty())
        {
            this.id = datasetId.trim();
        }
        else
        {
            this.id = "";
        }

        if (title != null && !title.trim().isEmpty())
        {
            this.title = title.trim();
        }
        else if (datasetId != null && !datasetId.trim().isEmpty())
        {
            this.title = datasetId.trim();
        }
        else if (testId != null && !testId.trim().isEmpty())
        {
            this.title = testId.trim();
        }
        else
        {
            this.title = "";
        }

        this.runId = runId != null ? runId : "";
        this.testName = (testName != null && !testName.isEmpty()) ? testName : (this.testClass + " " + this.title).trim();
        this.playbookFile = playbookFile != null ? playbookFile : "";
        this.testFile = testFile != null ? testFile : "";
        this.status = status != null ? status : "passed-clean";
        this.engine = engine != null ? engine : "Java";

        final String effectiveLocation;
        if (locale != null && !locale.trim().isEmpty())
        {
            effectiveLocation = locale.trim();
        }
        else if (location != null && !location.trim().isEmpty())
        {
            effectiveLocation = location.trim();
        }
        else
        {
            effectiveLocation = "UNKNOWN";
        }
        this.location = effectiveLocation;

        final String effectiveBrowser;
        if (browser != null && !browser.trim().isEmpty())
        {
            final String b = browser.trim();
            if ("chrome".equalsIgnoreCase(b))
            {
                effectiveBrowser = "Chrome";
            }
            else if ("firefox".equalsIgnoreCase(b))
            {
                effectiveBrowser = "Firefox";
            }
            else if ("edge".equalsIgnoreCase(b))
            {
                effectiveBrowser = "Edge";
            }
            else if ("safari".equalsIgnoreCase(b))
            {
                effectiveBrowser = "Safari";
            }
            else
            {
                effectiveBrowser = Character.toUpperCase(b.charAt(0)) + b.substring(1);
            }
        }
        else
        {
            effectiveBrowser = "Unknown";
        }
        this.browser = effectiveBrowser;
        this.failure = failure != null ? failure : "NONE";
        this.bugs = bugs != null ? new ArrayList<>(bugs) : new ArrayList<>();
        this.comment = comment;
        this.areaName = (areaName != null && !areaName.trim().isEmpty() && !"General".equalsIgnoreCase(areaName.trim())) ? areaName.trim() : "Browsing (default)";
        this.junitTags = junitTags != null ? new ArrayList<>(junitTags) : new ArrayList<>();
        this.dataBindings = dataBindings != null ? new HashMap<>(dataBindings) : new HashMap<>();
        this.localDataBindings = localDataBindings != null ? new HashMap<>(localDataBindings) : new HashMap<>();
        this.blocks = blocks;
        this.steps = steps;
        this.executionMode = (executionMode != null && !executionMode.isBlank()) ? executionMode : ((mode != null && !mode.isBlank()) ? mode : "FORCE_RECORDING");
        this.startTime = startTime != null ? startTime : "";
        this.dateFormatted = dateFormatted != null ? dateFormatted : "";
        this.timeFormatted = timeFormatted != null ? timeFormatted : "";
        this.timestampMs = timestampMs != null ? timestampMs : 0L;
        this.totalStepsCount = totalStepsCount != null ? totalStepsCount : 0;
        this.failedStepsCount = failedStepsCount != null ? failedStepsCount : 0;
        this.durationMs = durationMs != null ? durationMs : 0L;
        this.durationFormatted = durationFormatted != null ? durationFormatted : (this.durationMs > 0 ? String.format("%,d ms", this.durationMs) : "0 ms");
        this.llmCallsCount = llmCallsCount != null ? llmCallsCount : 0;
        this.llmTotalTokens = llmTotalTokens != null ? llmTotalTokens : 0L;
        this.llmCost = llmCost != null ? llmCost : 0.0;
        this.failureSnippet = failureSnippet != null ? failureSnippet : "";
    }

    public TestExecutionDto(
        final String id,
        final String runId,
        final String testClass,
        final String title,
        final String testName,
        final String playbookFile,
        final String testFile,
        final String status,
        final String engine,
        final String location,
        final String browser,
        final String failure,
        final List<String> bugs,
        final String comment,
        final String areaName,
        final List<String> junitTags,
        final Map<String, String> dataBindings,
        final Map<String, String> localDataBindings,
        final JsonNode blocks,
        final JsonNode steps)
    {
        this(id, runId, testClass, title, testName, playbookFile, testFile, status, engine, location, null, browser, failure, bugs, comment, areaName, junitTags, dataBindings, localDataBindings, blocks, steps, null, null, "FORCE_RECORDING", null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null);
    }

    public TestExecutionDto(
        final String id,
        final String testClass,
        final String title,
        final String status,
        final String engine,
        final String location,
        final String browser,
        final String failure,
        final List<String> bugs)
    {
        this(id, "", testClass, title, testClass + " " + title, "", "", status, engine, location, null, browser, failure, bugs, null, "Browsing (default)", new ArrayList<>(), new HashMap<>(), new HashMap<>(), null, null, null, null, "FORCE_RECORDING", null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null);
    }

    public String getId()
    {
        return id;
    }

    public String getRunId()
    {
        return runId;
    }

    public String getTestClass()
    {
        return testClass;
    }

    public String getTitle()
    {
        return title;
    }

    public String getTestName()
    {
        return testName;
    }

    public String getPlaybookFile()
    {
        return playbookFile;
    }

    public String getTestFile()
    {
        return testFile;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public String getEngine()
    {
        return engine;
    }

    public String getLocation()
    {
        return location;
    }

    public String getLocale()
    {
        return location;
    }

    public String getBrowser()
    {
        return browser;
    }

    public String getFailure()
    {
        return failure;
    }

    public List<String> getBugs()
    {
        return bugs;
    }

    public void setBugs(final List<String> bugs)
    {
        this.bugs.clear();
        if (bugs != null)
        {
            this.bugs.addAll(bugs);
        }
    }

    public String getComment()
    {
        return comment;
    }

    public String getAreaName()
    {
        return (areaName != null && !areaName.trim().isEmpty() && !"General".equalsIgnoreCase(areaName.trim())) ? areaName.trim() : "Browsing (default)";
    }

    public List<String> getJunitTags()
    {
        return junitTags;
    }

    public Map<String, String> getDataBindings()
    {
        return dataBindings;
    }

    public Map<String, String> getLocalDataBindings()
    {
        return localDataBindings;
    }

    public JsonNode getBlocks()
    {
        return blocks;
    }

    public JsonNode getSteps()
    {
        return steps;
    }

    public String getPrimaryBug()
    {
        return (bugs != null && !bugs.isEmpty()) ? bugs.get(0) : "";
    }

    public String getBlocksJson()
    {
        if (blocks != null && !blocks.isMissingNode() && !blocks.isNull())
        {
            try
            {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(blocks);
            }
            catch (final Exception ignored)
            {
            }
        }
        return "{}";
    }

    public String getStepsJson()
    {
        if (steps != null && !steps.isMissingNode() && !steps.isNull())
        {
            try
            {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(steps);
            }
            catch (final Exception ignored)
            {
            }
        }
        return "{}";
    }

    public String getLocalDataBindingsJson()
    {
        if (localDataBindings != null)
        {
            try
            {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(localDataBindings);
            }
            catch (final Exception ignored)
            {
            }
        }
        return "{}";
    }

    public String getExecutionMode()
    {
        return (executionMode != null && !executionMode.isBlank()) ? executionMode : "FORCE_RECORDING";
    }

    public String getMode()
    {
        return getExecutionMode();
    }

    public String getStartTime()
    {
        return startTime;
    }

    public String getDateFormatted()
    {
        return dateFormatted;
    }

    public String getTimeFormatted()
    {
        return timeFormatted;
    }

    public long getTimestampMs()
    {
        return timestampMs;
    }

    public int getTotalStepsCount()
    {
        return totalStepsCount;
    }

    public int getFailedStepsCount()
    {
        return failedStepsCount;
    }

    public long getDurationMs()
    {
        return durationMs;
    }

    public String getDurationFormatted()
    {
        if (durationFormatted != null && !durationFormatted.isBlank())
        {
            return durationFormatted;
        }
        return String.format("%,d ms", durationMs);
    }

    public int getLlmCallsCount()
    {
        return llmCallsCount;
    }

    public long getLlmTotalTokens()
    {
        return llmTotalTokens;
    }

    public String getLlmTotalTokensFormatted()
    {
        return String.format("%,d", llmTotalTokens);
    }

    public double getLlmCost()
    {
        return llmCost;
    }

    public String getLlmCostFormatted()
    {
        final double roundedUp = Math.ceil(llmCost * 10000.0) / 10000.0;
        return String.format("$%.4f", roundedUp);
    }

    public String getFailureSnippet()
    {
        return failureSnippet;
    }

    public int getHealedStepsCount()
    {
        return "succeeded-fixed".equalsIgnoreCase(status) ? 1 : 0;
    }

    public long getTotalTokensCount()
    {
        return llmTotalTokens;
    }

    public long getInputTokensCount()
    {
        return (long) (llmTotalTokens * 0.85);
    }

    public long getOutputTokensCount()
    {
        return (long) (llmTotalTokens * 0.15);
    }

    public long getCachedTokensCount()
    {
        return 0L;
    }

    public String getEstimatedCostUsd()
    {
        return getLlmCostFormatted();
    }

    public String getFormattedTime()
    {
        if (dateFormatted != null && !dateFormatted.isBlank() && timeFormatted != null && !timeFormatted.isBlank())
        {
            return dateFormatted + " " + timeFormatted;
        }
        return startTime != null ? startTime : "";
    }

    public boolean isHasVisualChanges()
    {
        return false;
    }

    public boolean getHasVisualChanges()
    {
        return false;
    }

    public String getVideoPath()
    {
        return "";
    }

    public String getLogPath()
    {
        return "";
    }

    public long getStartTimeMs()
    {
        return timestampMs;
    }

    public String getStartTimeDate()
    {
        return dateFormatted != null && !dateFormatted.isBlank() ? dateFormatted : "2026-08-20";
    }

    public String getStartTimeClock()
    {
        return timeFormatted != null && !timeFormatted.isBlank() ? timeFormatted : "12:31:21";
    }
}
