package com.xceptance.neodymium.common.xtc.dto;

// Data Transfer Object for creating a new test run in the XTC API.
public class CreateRunRequest
{
    private final String startedAt;

    private final Integer estimatedDuration;

    private final String name;

    private final String testInstance;

    private final String profile;

    private final String link;

    private final String buildNumber;

    private final String description;

    public CreateRunRequest(String startedAt, Integer estimatedDuration, String name, String testInstance, String profile, String link, String buildNumber,
                            String description)
    {
        this.startedAt = startedAt;
        this.estimatedDuration = estimatedDuration;
        this.name = name;
        this.testInstance = testInstance;
        this.profile = profile;
        this.link = link;
        this.buildNumber = buildNumber;
        this.description = description;
    }

    public String getStartedAt()
    {
        return startedAt;
    }

    public Integer getEstimatedDuration()
    {
        return estimatedDuration;
    }

    public String getName()
    {
        return name;
    }

    public String getTestInstance()
    {
        return testInstance;
    }

    public String getProfile()
    {
        return profile;
    }

    public String getLink()
    {
        return link;
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public String getDescription()
    {
        return description;
    }
}
