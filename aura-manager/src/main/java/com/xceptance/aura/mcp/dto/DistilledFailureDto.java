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
