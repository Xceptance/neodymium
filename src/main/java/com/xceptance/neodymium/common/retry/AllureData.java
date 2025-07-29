package com.xceptance.neodymium.common.retry;

public class AllureData
{
    private long start;

    private String uuid;

    private String historyId;

    private String labelId;

    private String parameterId;

    public long getStart()
    {
        return start;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public void setHistoryId(String historyId)
    {
        this.historyId = historyId;
    }

    public void setLabelId(String labelId)
    {
        this.labelId = labelId;
    }

    public void setParameterId(String parameterId)
    {
        this.parameterId = parameterId;
    }

    public String getUuid()
    {
        return uuid;
    }

    public String getHistoryId()
    {
        return historyId;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public String getParameterId()
    {
        return parameterId;
    }

    public void setStart(long start)
    {
        this.start = start;
    }
}
