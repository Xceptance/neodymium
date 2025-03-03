package com.xceptance.neodymium.junit4.statement.repeat;

public class RepeatOnFailureData
{
    private int iterationNumber;

    private int maxNumber;

    public RepeatOnFailureData(int iterationNumber, int maxNumber)
    {
        this.iterationNumber = iterationNumber;
        this.maxNumber = maxNumber;
    }

    public int getIterationNumber()
    {
        return iterationNumber;
    }

    public int getMaxNumber()
    {
        return maxNumber;
    }
}
