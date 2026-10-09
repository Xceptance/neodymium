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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.xceptance.aura.report.service.AuraReportDataService;
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
    private String id;
    private final String runId;
    private final String testClass;
    private final String testMethod;
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
    private JsonNode blocks;
    private JsonNode steps;
    private String executionMode;
    private final String startTime;
    private final String dateFormatted;
    private final String timeFormatted;
    private final long timestampMs;
    private final int totalStepsCount;
    private final int failedStepsCount;
    private int healedStepsCount;
    private Boolean healed;
    private final long durationMs;
    private final String durationFormatted;
    private final int llmCallsCount;
    private final long llmTotalTokens;
    private final double llmCost;
    private final String failureSnippet;
    private final String failureReason;
    private final String failureStackTrace;
    private final String visualRcaExplanation;
    private final JsonNode llmResponsibility;
    private final JsonNode metrics;
    private final String videoUrl;
    private final JsonNode linterFindings;
    private final JsonNode postFlightFindings;

    public TestExecutionDto()
    {
        this("", "", "", "", "", "", "", "", "passed-clean", "Java", "UNKNOWN", null, "Unknown", "NONE", new ArrayList<>(), null, "Browsing (default)", new ArrayList<>(), new HashMap<>(), new HashMap<>(), null, null, null, null, null, null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null, "", "", "", null, null, "", false, null, null);
    }

    @JsonCreator
    public TestExecutionDto(
        @JsonProperty("id") final String id,
        @JsonProperty("runId") final String runId,
        @JsonProperty("testClass") final String testClass,
        @JsonProperty("testMethod") final String testMethod,
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
        @JsonProperty("failureSnippet") final String failureSnippet,
        @JsonProperty("failureReason") @JsonAlias({"error", "errorMessage", "failure"}) final String failureReason,
        @JsonProperty("failureStackTrace") @JsonAlias({"failureStacktrace", "stackTrace", "stacktrace", "trace"}) final String failureStackTrace,
        @JsonProperty("visualRcaExplanation") final String visualRcaExplanation,
        @JsonProperty("llmResponsibility") final JsonNode llmResponsibility,
        @JsonProperty("metrics") final JsonNode metrics,
        @JsonProperty("videoUrl") @JsonAlias({"videoPath", "video"}) final String videoUrl,
        @JsonProperty("healed") final Boolean healed,
        @JsonProperty("linterFindings") final JsonNode linterFindings,
        @JsonProperty("postFlightFindings") final JsonNode postFlightFindings)
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
        this.playbookFile = playbookFile != null ? playbookFile : "";
        this.testFile = testFile != null ? testFile : "";
        this.junitTags = junitTags != null ? new ArrayList<>(junitTags) : new ArrayList<>();

        final String effectiveTestMethod;
        if (testMethod != null && !testMethod.trim().isEmpty() && !"executeTest".equalsIgnoreCase(testMethod.trim()))
        {
            effectiveTestMethod = testMethod.trim();
        }
        else if (this.junitTags.size() >= 2 && !this.junitTags.get(1).trim().isEmpty() && !this.junitTags.get(1).trim().startsWith("Dataset:") && !this.junitTags.get(1).trim().startsWith("Location:") && !this.junitTags.get(1).trim().startsWith("Browser:") && !this.junitTags.get(1).trim().equalsIgnoreCase(this.testClass) && !"executeTest".equalsIgnoreCase(this.junitTags.get(1).trim()))
        {
            effectiveTestMethod = this.junitTags.get(1).trim();
        }
        else if (this.testFile.contains("#"))
        {
            final String tm = this.testFile.substring(this.testFile.indexOf('#') + 1).trim();
            effectiveTestMethod = (!tm.isEmpty() && !"executeTest".equalsIgnoreCase(tm)) ? tm : "";
        }
        else if (this.id != null && (this.id.contains("#") || this.id.contains("%23")))
        {
            String decodedId = this.id;
            if (decodedId.contains("%23"))
            {
                try
                {
                    decodedId = java.net.URLDecoder.decode(decodedId, java.nio.charset.StandardCharsets.UTF_8);
                }
                catch (final Exception ignored)
                {
                }
            }
            final String[] parts = decodedId.split("#");
            final String tm = (parts.length > 1 && !parts[1].trim().isEmpty()) ? parts[1].trim() : "";
            effectiveTestMethod = !"executeTest".equalsIgnoreCase(tm) ? tm : "";
        }
        else
        {
            effectiveTestMethod = "";
        }
        this.testMethod = effectiveTestMethod;

        this.testName = (testName != null && !testName.isEmpty()) ? testName : (this.testClass + " " + (this.testMethod.isEmpty() ? this.title : (this.testMethod + " [" + this.title + "]"))).trim();
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

        this.browser = AuraReportDataService.normalizeBrowser(browser);
        this.failure = failure != null ? failure : "NONE";
        this.bugs = bugs != null ? new ArrayList<>(bugs) : new ArrayList<>();
        this.comment = comment;
        this.areaName = (areaName != null && !areaName.trim().isEmpty() && !"General".equalsIgnoreCase(areaName.trim())) ? areaName.trim() : "Browsing (default)";
        this.dataBindings = dataBindings != null ? new HashMap<>(dataBindings) : new HashMap<>();
        this.localDataBindings = localDataBindings != null ? new HashMap<>(localDataBindings) : new HashMap<>();
        this.blocks = blocks;
        if (this.blocks != null)
        {
            getHealedStepsCount();
        }
        this.steps = steps;
        this.executionMode = (executionMode != null && !executionMode.isBlank()) ? executionMode : ((mode != null && !mode.isBlank()) ? mode : null);
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
        this.failureReason = failureReason != null ? failureReason : "";
        this.failureStackTrace = failureStackTrace != null ? failureStackTrace : "";
        this.visualRcaExplanation = visualRcaExplanation != null ? visualRcaExplanation : "";
        this.llmResponsibility = llmResponsibility;
        this.metrics = metrics;
        this.videoUrl = videoUrl != null ? videoUrl : "";
        this.healed = Boolean.TRUE.equals(healed);
        this.linterFindings = linterFindings;
        this.postFlightFindings = postFlightFindings;
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
        final String locale,
        final String browser,
        final String failure,
        final List<String> bugs,
        final String comment,
        final String areaName,
        final List<String> junitTags,
        final Map<String, String> dataBindings,
        final Map<String, String> localDataBindings,
        final JsonNode blocks,
        final JsonNode steps,
        final String testId,
        final String datasetId,
        final String executionMode,
        final String mode,
        final String startTime,
        final String dateFormatted,
        final String timeFormatted,
        final Long timestampMs,
        final Integer totalStepsCount,
        final Integer failedStepsCount,
        final Long durationMs,
        final String durationFormatted,
        final Integer llmCallsCount,
        final Long llmTotalTokens,
        final Double llmCost,
        final String failureSnippet)
    {
        this(id, runId, testClass, null, title, testName, playbookFile, testFile, status, engine, location, locale, browser, failure, bugs, comment, areaName, junitTags, dataBindings, localDataBindings, blocks, steps, testId, datasetId, executionMode, mode, startTime, dateFormatted, timeFormatted, timestampMs, totalStepsCount, failedStepsCount, durationMs, durationFormatted, llmCallsCount, llmTotalTokens, llmCost, failureSnippet, "", "", "", null, null, "", false, null, null);
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
        final String locale,
        final String browser,
        final String failure,
        final List<String> bugs,
        final String comment,
        final String areaName,
        final List<String> junitTags,
        final Map<String, String> dataBindings,
        final Map<String, String> localDataBindings,
        final JsonNode blocks,
        final JsonNode steps,
        final String testId,
        final String datasetId,
        final String executionMode,
        final String mode,
        final String startTime,
        final String dateFormatted,
        final String timeFormatted,
        final Long timestampMs,
        final Integer totalStepsCount,
        final Integer failedStepsCount,
        final Long durationMs,
        final String durationFormatted,
        final Integer llmCallsCount,
        final Long llmTotalTokens,
        final Double llmCost,
        final String failureSnippet,
        final String failureReason,
        final String failureStackTrace,
        final String visualRcaExplanation)
    {
        this(id, runId, testClass, null, title, testName, playbookFile, testFile, status, engine, location, locale, browser, failure, bugs, comment, areaName, junitTags, dataBindings, localDataBindings, blocks, steps, testId, datasetId, executionMode, mode, startTime, dateFormatted, timeFormatted, timestampMs, totalStepsCount, failedStepsCount, durationMs, durationFormatted, llmCallsCount, llmTotalTokens, llmCost, failureSnippet, failureReason, failureStackTrace, visualRcaExplanation, null, null, "", false, null, null);
    }

    public TestExecutionDto(
        final String id,
        final String runId,
        final String testClass,
        final String testMethod,
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
        this(id, runId, testClass, testMethod, title, testName, playbookFile, testFile, status, engine, location, null, browser, failure, bugs, comment, areaName, junitTags, dataBindings, localDataBindings, blocks, steps, null, null, null, null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null, "", "", "", null, null, "", false, null, null);
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
        this(id, runId, testClass, null, title, testName, playbookFile, testFile, status, engine, location, null, browser, failure, bugs, comment, areaName, junitTags, dataBindings, localDataBindings, blocks, steps, null, null, null, null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null, "", "", "", null, null, "", false, null, null);
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
        this(id, "", testClass, null, title, testClass + " " + title, "", "", status, engine, location, null, browser, failure, bugs, null, "Browsing (default)", new ArrayList<>(), new HashMap<>(), new HashMap<>(), null, null, null, null, null, null, null, null, null, 0L, 0, 0, 0L, null, 0, 0L, 0.0, null, "", "", "", null, null, "", false, null, null);
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id != null ? id.trim() : "";
    }

    public String getRunId()
    {
        return runId;
    }

    public String getTestClass()
    {
        return testClass;
    }

    public String getTestMethod()
    {
        return testMethod != null ? testMethod : "";
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

    /**
     * Computes normalized status key for UI filtering and badge styling.
     *
     * @return normalized status key (RUNNING, PASSED, SUCCEEDED_FIXED, FAILED_KNOWN, FAILED_UNKNOWN, SKIPPED)
     */
    public String getDisplayStatusKey()
    {
        final String st = status != null ? status.toLowerCase() : "running";
        if ("running".equals(st) || "in_progress".equals(st) || "executing".equals(st) || "pending".equals(st))
        {
            return "RUNNING";
        }
        final boolean hasBugs = bugs != null && !bugs.isEmpty();
        if ("succeeded-fixed".equals(st) || "fixed".equals(st) || (("passed-clean".equals(st) || "passed".equals(st) || "succeeded".equals(st)) && hasBugs))
        {
            return "SUCCEEDED_FIXED";
        }
        if ("passed-clean".equals(st) || "passed".equals(st) || "succeeded".equals(st) || "healed".equals(st))
        {
            return "PASSED";
        }
        if ("failed-known".equals(st) || "known".equals(st) || (("failed".equals(st) || "error".equals(st) || "failure".equals(st)) && hasBugs))
        {
            return "FAILED_KNOWN";
        }
        if ("ignored".equals(st) || "skipped".equals(st))
        {
            return "SKIPPED";
        }
        return hasBugs ? "FAILED_KNOWN" : "FAILED_UNKNOWN";
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

    public void setBlocks(final JsonNode blocks)
    {
        if (blocks != null)
        {
            getHealedStepsCount();
        }
        this.blocks = blocks;
    }

    public void setSteps(final JsonNode steps)
    {
        this.steps = steps;
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
        return executionMode;
    }

    public void setExecutionMode(final String executionMode)
    {
        this.executionMode = executionMode;
    }

    public String getMode()
    {
        return getDisplayMode();
    }

    /**
     * Resolves the user-facing display mode for the All Tests tab mode column.
     * <p>
     * - If REPLAY_WITH_HEALING (or AUTO) and healing was performed: HEALED
     * - If REPLAY_WITH_HEALING (or AUTO or REPLAY_STRICT) and no healing was performed: PLAYBOOK
     * - If LLM_RECORDING (or LLM_ONLY or FORCE_RECORDING): AI
     * - If no mode was configured or test is standard non-AI: JAVA
     *
     * @return display mode string (PLAYBOOK, HEALED, AI, or JAVA)
     */
    @JsonProperty("displayMode")
    public String getDisplayMode()
    {
        final String rawMode = this.executionMode != null ? this.executionMode.trim() : "";
        if (rawMode.isEmpty() || "JAVA".equalsIgnoreCase(rawMode))
        {
            return "JAVA";
        }
        final String upper = rawMode.toUpperCase();
        if ("REPLAY_WITH_HEALING".equals(upper) || "AUTO".equals(upper))
        {
            return isHealed() ? "HEALED" : "PLAYBOOK";
        }
        if ("REPLAY_STRICT".equals(upper) || "PLAYBOOK".equals(upper))
        {
            return "PLAYBOOK";
        }
        if ("LLM_RECORDING".equals(upper) || "LLM_ONLY".equals(upper) || "FORCE_RECORDING".equals(upper) || "AI".equals(upper))
        {
            return "AI";
        }
        if (isHealed())
        {
            return "HEALED";
        }
        return upper;
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
        if (timestampMs > 0L)
        {
            return timestampMs;
        }
        if (startTime != null && !startTime.isBlank())
        {
            final Long parsed = com.xceptance.aura.report.service.RunStorageSyncService.parseTimestampToMs(startTime);
            if (parsed != null && parsed > 0L)
            {
                return parsed;
            }
        }
        return 0L;
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
        return String.format(java.util.Locale.ROOT, "%.4f", roundedUp);
    }

    public String getFailureSnippet()
    {
        return failureSnippet;
    }

    @JsonProperty("healedStepsCount")
    public void setHealedStepsCount(final int healedStepsCount)
    {
        this.healedStepsCount = healedStepsCount;
    }

    public int getHealedStepsCount()
    {
        if (healedStepsCount > 0)
        {
            return healedStepsCount;
        }
        if (blocks != null)
        {
            int count = 0;
            for (final String key : List.of("before", "steps", "after"))
            {
                final JsonNode arr = blocks.path(key);
                if (arr.isArray())
                {
                    for (final JsonNode stepNode : arr)
                    {
                        if ("healed".equalsIgnoreCase(stepNode.path("status").asText("")) || stepNode.path("healed").asBoolean(false))
                        {
                            count++;
                        }
                    }
                }
            }
            if (count > 0)
            {
                this.healedStepsCount = count;
                return count;
            }
        }
        return "healed".equalsIgnoreCase(status) ? 1 : 0;
    }

    @JsonProperty("healed")
    public void setHealed(final Boolean healed)
    {
        this.healed = healed;
    }

    @JsonProperty("healed")
    public boolean isHealed()
    {
        return Boolean.TRUE.equals(healed) || getHealedStepsCount() > 0 || "healed".equalsIgnoreCase(status);
    }

    public boolean isPassedHealed()
    {
        if (!isHealed())
        {
            return false;
        }
        final String st = getDisplayStatusKey();
        return "PASSED".equals(st) || "SUCCEEDED_FIXED".equals(st);
    }

    public boolean isPassHealed()
    {
        return isHealed() && "PASSED".equals(getDisplayStatusKey());
    }

    public boolean isFixedHealed()
    {
        return isHealed() && "SUCCEEDED_FIXED".equals(getDisplayStatusKey());
    }

    public boolean isFailedHealed()
    {
        if (!isHealed())
        {
            return false;
        }
        final String st = getDisplayStatusKey();
        return "FAILED_KNOWN".equals(st) || "FAILED_UNKNOWN".equals(st);
    }

    public boolean isKnownHealed()
    {
        return isHealed() && "FAILED_KNOWN".equals(getDisplayStatusKey());
    }

    public boolean isUnknownHealed()
    {
        return isHealed() && "FAILED_UNKNOWN".equals(getDisplayStatusKey());
    }

    @JsonProperty("aiDriven")
    public boolean isAiDriven()
    {
        return "AI".equalsIgnoreCase(getDisplayMode())
            || (llmCallsCount > 0)
            || "LLM_RECORDING".equalsIgnoreCase(this.executionMode)
            || "LLM_ONLY".equalsIgnoreCase(this.executionMode);
    }

    public boolean isPassAi()
    {
        return isAiDriven() && "PASSED".equals(getDisplayStatusKey());
    }

    public boolean isFixedAi()
    {
        return isAiDriven() && "SUCCEEDED_FIXED".equals(getDisplayStatusKey());
    }

    public boolean isKnownAi()
    {
        return isAiDriven() && "FAILED_KNOWN".equals(getDisplayStatusKey());
    }

    public boolean isUnknownAi()
    {
        return isAiDriven() && "FAILED_UNKNOWN".equals(getDisplayStatusKey());
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

    public String getLlmResponsibilityJson()
    {
        if (llmResponsibility == null || llmResponsibility.isNull())
        {
            return "";
        }
        return llmResponsibility.toString();
    }

    public String getContextLevelCountsJson()
    {
        if (metrics == null || metrics.isNull())
        {
            return "";
        }
        final JsonNode counts = metrics.path("contextLevelCounts");
        if (counts == null || counts.isNull() || !counts.isObject() || counts.isEmpty())
        {
            return "";
        }
        return counts.toString();
    }

    public String getFormattedTime()
    {
        if (timestampMs > 0L)
        {
            final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMs),
                java.time.ZoneId.systemDefault()
            );
            return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
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

    public String getVideoUrl()
    {
        return videoUrl != null ? videoUrl : "";
    }

    public String getVideoPath()
    {
        return videoUrl != null ? videoUrl : "";
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

    public String getFailureReason()
    {
        return failureReason != null ? failureReason : "";
    }

    public String getFailureStackTrace()
    {
        return failureStackTrace != null ? failureStackTrace : "";
    }

    public String getVisualRcaExplanation()
    {
        return visualRcaExplanation != null ? visualRcaExplanation : "";
    }

    @JsonIgnore
    public JsonNode getLinterFindings()
    {
        return linterFindings;
    }

    @JsonIgnore
    public JsonNode getPostFlightFindings()
    {
        return postFlightFindings;
    }

    @JsonProperty("linterFindings")
    @JsonRawValue
    public String getLinterFindingsRaw()
    {
        return (linterFindings != null && !linterFindings.isNull()) ? linterFindings.toString() : null;
    }

    @JsonProperty("postFlightFindings")
    @JsonRawValue
    public String getPostFlightFindingsRaw()
    {
        return (postFlightFindings != null && !postFlightFindings.isNull()) ? postFlightFindings.toString() : null;
    }

    public String getLinterFindingsJson()
    {
        return (linterFindings != null && !linterFindings.isNull()) ? linterFindings.toString() : "";
    }

    public String getPostFlightFindingsJson()
    {
        return (postFlightFindings != null && !postFlightFindings.isNull()) ? postFlightFindings.toString() : "";
    }
}
