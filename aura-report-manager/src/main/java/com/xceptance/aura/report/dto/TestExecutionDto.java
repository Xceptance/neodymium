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

    public TestExecutionDto()
    {
        this("", "", "", "", "", "", "", "passed-clean", "Java", "US", "Chrome", "NONE", new ArrayList<>(), null, "General", new ArrayList<>(), new HashMap<>(), new HashMap<>(), null, null);
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
        @JsonProperty("browser") final String browser,
        @JsonProperty("failure") final String failure,
        @JsonProperty("bugs") final List<String> bugs,
        @JsonProperty("comment") final String comment,
        @JsonProperty("areaName") final String areaName,
        @JsonProperty("junitTags") final List<String> junitTags,
        @JsonProperty("dataBindings") final Map<String, String> dataBindings,
        @JsonProperty("localDataBindings") final Map<String, String> localDataBindings,
        @JsonProperty("blocks") final JsonNode blocks,
        @JsonProperty("steps") final JsonNode steps)
    {
        this.id = id != null ? id : "";
        this.runId = runId != null ? runId : "";
        this.testClass = testClass != null ? testClass : "";
        this.title = title != null ? title : "";
        this.testName = (testName != null && !testName.isEmpty()) ? testName : (this.testClass + " " + this.title).trim();
        this.playbookFile = playbookFile != null ? playbookFile : "";
        this.testFile = testFile != null ? testFile : "";
        this.status = status != null ? status : "passed-clean";
        this.engine = engine != null ? engine : "Java";
        this.location = location != null ? location : "US";
        this.browser = browser != null ? browser : "Chrome";
        this.failure = failure != null ? failure : "NONE";
        this.bugs = bugs != null ? new ArrayList<>(bugs) : new ArrayList<>();
        this.comment = comment;
        this.areaName = (areaName != null && !areaName.trim().isEmpty()) ? areaName.trim() : "General";
        this.junitTags = junitTags != null ? new ArrayList<>(junitTags) : new ArrayList<>();
        this.dataBindings = dataBindings != null ? new HashMap<>(dataBindings) : new HashMap<>();
        this.localDataBindings = localDataBindings != null ? new HashMap<>(localDataBindings) : new HashMap<>();
        this.blocks = blocks;
        this.steps = steps;
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
        this(id, "", testClass, title, testClass + " " + title, "", "", status, engine, location, browser, failure, bugs, null, "General", new ArrayList<>(), new HashMap<>(), new HashMap<>(), null, null);
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
        return (areaName != null && !areaName.trim().isEmpty()) ? areaName.trim() : "General";
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
}
