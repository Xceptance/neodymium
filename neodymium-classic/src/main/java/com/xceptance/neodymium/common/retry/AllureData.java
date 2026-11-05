package com.xceptance.neodymium.common.retry;

/**
 * Object to store Allure properties of the test. Used in {@link RetryMethodData}
 */
public class AllureData
{
    private long start;

    private String historyId;

    private String labelId;

    public long getStart()
    {
        return start;
    }

    public void setHistoryId(String historyId)
    {
        this.historyId = historyId;
    }

    public void setLabelId(String labelId)
    {
        this.labelId = labelId;
    }

    public String getHistoryId()
    {
        return historyId;
    }

    public String getLabelId()
    {
        return labelId;
    }

    public void setStart(long start)
    {
        this.start = start;
    }
}
