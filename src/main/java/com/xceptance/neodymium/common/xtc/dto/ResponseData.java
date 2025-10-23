package com.xceptance.neodymium.common.xtc.dto;

// Data Transfer Object for the response data of a test run in the XTC API.
public class ResponseData
{
    private final String name;

    private final String index;

    private final String description;

    private final String startedAt;

    private final String finishedAt;

    private final String state;

    private final String summary;

    private final String rating;

    public ResponseData(String index, String name, String startedAt, String finishedAt, String state, String summary, String description,
                                 String rating)
    {
        this.index = index;
        this.name = name;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.state = state;
        this.summary = summary;
        this.description = description;
        this.rating = rating;
    }

    public String getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public String getStartedAt()
    {
        return startedAt;
    }

    public String getFinishedAt()
    {
        return finishedAt;
    }

    public String getState()
    {
        return state;
    }

    public String getSummary()
    {
        return summary;
    }

    public String getDescription()
    {
        return description;
    }

    public String getRating()
    {
        return rating;
    }
}
