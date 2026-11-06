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
package com.xceptance.aura.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representation of a distilled test failure context capped at 1,500 characters.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistilledFailureDto
{
    private String testName;

    private String stepText;

    private String locator;

    private String currentUrl;

    private String domSnippet;

    private String cleanedStackTrace;

    private String clusterCategory;

    public DistilledFailureDto()
    {
    }

    public String getTestName()
    {
        return testName;
    }

    public void setTestName(final String testName)
    {
        this.testName = testName;
    }

    public String getStepText()
    {
        return stepText;
    }

    public void setStepText(final String stepText)
    {
        this.stepText = stepText;
    }

    public String getLocator()
    {
        return locator;
    }

    public void setLocator(final String locator)
    {
        this.locator = locator;
    }

    public String getCurrentUrl()
    {
        return currentUrl;
    }

    public void setCurrentUrl(final String currentUrl)
    {
        this.currentUrl = currentUrl;
    }

    public String getDomSnippet()
    {
        return domSnippet;
    }

    public void setDomSnippet(final String domSnippet)
    {
        this.domSnippet = domSnippet;
    }

    public String getCleanedStackTrace()
    {
        return cleanedStackTrace;
    }

    public void setCleanedStackTrace(final String cleanedStackTrace)
    {
        this.cleanedStackTrace = cleanedStackTrace;
    }

    public String getClusterCategory()
    {
        return clusterCategory;
    }

    public void setClusterCategory(final String clusterCategory)
    {
        this.clusterCategory = clusterCategory;
    }
}
