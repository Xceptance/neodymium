/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data transfer object encapsulating the lifecycle state transition of a test variation
 * across multiple historical test execution reports.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TestTransitionDto
{
    /**
     * Enumeration of test lifecycle transition categories across the analyzed reports.
     */
    public enum TransitionType
    {
        /**
         * Test was failing in prior report(s) and has since passed in the latest report.
         */
        TURNED_GREEN,

        /**
         * Test is currently failing and lacks an associated bug ticket (action required).
         */
        UNTICKETED_FAIL,

        /**
         * Test is currently failing and already has an associated bug ticket.
         */
        KNOWN_FAIL,

        /**
         * Test previously passed but newly failed in the latest report (regression).
         */
        REGRESSION,

        /**
         * Test passed across all analyzed reports.
         */
        STABLE_PASS,

        /**
         * Test flips intermittently between passing and failing across reports.
         */
        INTERMITTENT_FLAKY
    }

    private final String key;
    private final String variationId;
    private final String testClass;
    private final String testMethod;
    private final String title;
    private final String browser;
    private final String location;
    private final TransitionType transition;
    private final String latestStatus;
    private final String latestRunId;
    private final String latestExecutionId;
    private final String previousFailedRunId;
    private final String previousStatus;
    private final List<String> bugs;
    private final List<String> bugSeverities;
    private final String errorCategory;
    private final String distilledError;
    private final String locator;
    private final List<String> tags;

    public TestTransitionDto(
        final String key,
        final String variationId,
        final String testClass,
        final String testMethod,
        final String title,
        final String browser,
        final String location,
        final TransitionType transition,
        final String latestStatus,
        final String latestRunId,
        final String latestExecutionId,
        final String previousFailedRunId,
        final String previousStatus,
        final List<String> bugs,
        final List<String> bugSeverities,
        final String errorCategory,
        final String distilledError,
        final String locator,
        final List<String> tags)
    {
        this.key = (key != null) ? key : "";
        this.variationId = (variationId != null) ? variationId : "";
        this.testClass = (testClass != null) ? testClass : "";
        this.testMethod = (testMethod != null) ? testMethod : "";
        this.title = (title != null) ? title : "";
        this.browser = (browser != null) ? browser : "";
        this.location = (location != null) ? location : "";
        this.transition = (transition != null) ? transition : TransitionType.STABLE_PASS;
        this.latestStatus = (latestStatus != null) ? latestStatus : "";
        this.latestRunId = (latestRunId != null) ? latestRunId : "";
        this.latestExecutionId = (latestExecutionId != null) ? latestExecutionId : "";
        this.previousFailedRunId = (previousFailedRunId != null) ? previousFailedRunId : "";
        this.previousStatus = (previousStatus != null) ? previousStatus : "";
        this.bugs = (bugs != null && !bugs.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(bugs))
            : Collections.emptyList();
        this.bugSeverities = (bugSeverities != null && !bugSeverities.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(bugSeverities))
            : Collections.emptyList();
        this.errorCategory = (errorCategory != null) ? errorCategory : "";
        this.distilledError = (distilledError != null) ? distilledError : "";
        this.locator = (locator != null) ? locator : "";
        this.tags = (tags != null && !tags.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(tags))
            : Collections.emptyList();
    }

    public String getKey()
    {
        return key;
    }

    public String getVariationId()
    {
        return variationId;
    }

    public String getTestClass()
    {
        return testClass;
    }

    public String getTestMethod()
    {
        return testMethod;
    }

    public String getTitle()
    {
        return title;
    }

    public String getBrowser()
    {
        return browser;
    }

    public String getLocation()
    {
        return location;
    }

    public TransitionType getTransition()
    {
        return transition;
    }

    public String getLatestStatus()
    {
        return latestStatus;
    }

    public String getLatestRunId()
    {
        return latestRunId;
    }

    public String getLatestExecutionId()
    {
        return latestExecutionId;
    }

    public String getPreviousFailedRunId()
    {
        return previousFailedRunId;
    }

    @JsonIgnore
    public String getPreviousRunId()
    {
        return previousFailedRunId;
    }

    @JsonIgnore
    public String getPriorFailedRunId()
    {
        return previousFailedRunId;
    }

    public String getPreviousStatus()
    {
        return previousStatus;
    }

    @JsonIgnore
    public String getPriorFailedStatus()
    {
        return previousStatus;
    }

    public List<String> getBugs()
    {
        return bugs;
    }

    @JsonIgnore
    public List<String> getBugTickets()
    {
        return bugs;
    }

    @JsonIgnore
    public boolean hasBugs()
    {
        return !bugs.isEmpty();
    }

    public List<String> getBugSeverities()
    {
        return bugSeverities;
    }

    public String getErrorCategory()
    {
        return errorCategory;
    }

    @JsonIgnore
    public String getDistilledErrorCategory()
    {
        return errorCategory;
    }

    public String getTestName()
    {
        if (testClass != null && !testClass.isEmpty() && testMethod != null && !testMethod.isEmpty())
        {
            return testClass + " " + testMethod;
        }
        if (title != null && !title.isEmpty())
        {
            return title;
        }
        return (key != null) ? key : "";
    }

    public String getDistilledError()
    {
        return distilledError;
    }

    @JsonIgnore
    public String getCleanedStackTrace()
    {
        return distilledError;
    }

    public String getLocator()
    {
        return locator;
    }

    @JsonIgnore
    public String getDistilledLocator()
    {
        return locator;
    }

    public List<String> getTags()
    {
        return tags;
    }
}
