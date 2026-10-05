package com.xceptance.aura.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Representation of a grouped failure cluster.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FailureClusterDto
{
    private String category;

    private int count;

    private String sampleMessage;

    private List<String> affectedSuites;

    private String representativeFailure;

    public FailureClusterDto()
    {
    }

    public FailureClusterDto(final String category, final int count, final String sampleMessage,
                             final List<String> affectedSuites, final String representativeFailure)
    {
        this.category = category;
        this.count = count;
        this.sampleMessage = sampleMessage;
        this.affectedSuites = affectedSuites;
        this.representativeFailure = representativeFailure;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(final String category)
    {
        this.category = category;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(final int count)
    {
        this.count = count;
    }

    public String getSampleMessage()
    {
        return sampleMessage;
    }

    public void setSampleMessage(final String sampleMessage)
    {
        this.sampleMessage = sampleMessage;
    }

    public List<String> getAffectedSuites()
    {
        return affectedSuites;
    }

    public void setAffectedSuites(final List<String> affectedSuites)
    {
        this.affectedSuites = affectedSuites;
    }

    public String getRepresentativeFailure()
    {
        return representativeFailure;
    }

    public void setRepresentativeFailure(final String representativeFailure)
    {
        this.representativeFailure = representativeFailure;
    }
}
