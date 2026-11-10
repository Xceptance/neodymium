package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;

import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateAllDataSets;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionFirstParameterHigherMaxRange;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionFirstParameterLowerMinRange;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionMoreThanTwoParameters;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionNoDataSetFound;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionOnlyParameterHigherMaxRange;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionOnlyParameterLowerMinRange;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionSecondParameterHigherMaxRange;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateDataSetExceptionSecondParameterLowerMinRange;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateMultipleDataSets;
import com.xceptance.neodymium.junit4.testclasses.data.annotation.InstantiateSingleDataSet;

import org.junit.runner.Result;

public class TestDataSetTest extends NeodymiumTest
{
    @Test
    public void testNoDataSetsFound()
    {
        Result result = run(InstantiateDataSetExceptionNoDataSetFound.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: No data sets were found at all regarding your test case, please make sure to reference everything correctly.");
    }

    @Test
    public void testInstantiateAllDataSets()
    {
        Result result = run(InstantiateAllDataSets.class);
        checkPass(result, 10, 0);
    }

    @Test
    public void testInstantiateSingleDataSet()
    {
        Result result = run(InstantiateSingleDataSet.class);
        checkPass(result, 10, 0);
    }

    @Test
    public void testInstantiateMultipleDataSets()
    {
        Result result = run(InstantiateMultipleDataSets.class);
        checkPass(result, 10, 0);
    }

    @Test
    public void testDataSetExceptionMoreThanTwoParameters()
    {
        Result result = run(InstantiateDataSetExceptionMoreThanTwoParameters.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Only a range of 1-2 parameters are permitted using the DataSet annotation, please adjust your DataSet annotation accordingly.");
    }

    @Test
    public void testDataSetExceptionOnlyParameterLowerMinRange()
    {
        Result result = run(InstantiateDataSetExceptionOnlyParameterLowerMinRange.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 0, but there are only 1-5 available.");
    }

    @Test
    public void testDataSetExceptionOnlyParameterHigherMaxRange()
    {
        Result result = run(InstantiateDataSetExceptionOnlyParameterHigherMaxRange.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 6, but there are only 1-5 available.");
    }

    @Test
    public void testDataSetExceptionFirstParameterLowerMinRange()
    {
        Result result = run(InstantiateDataSetExceptionFirstParameterLowerMinRange.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 0, but there are only 1-5 available.");
    }

    @Test
    public void testDataSetExceptionFirstParameterHigherMaxRange()
    {
        Result result = run(InstantiateDataSetExceptionFirstParameterHigherMaxRange.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 6, but there are only 1-5 available.");
    }

    @Test
    public void testDataSetExceptionSecondParameterLowerMinRange()
    {
        Result result = run(InstantiateDataSetExceptionSecondParameterLowerMinRange.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 0, but there are only 1-5 available.");
    }

    @Test
    public void testDataSetExceptionSecondParameterHigherMaxRange()
    {
        Result result = run(InstantiateDataSetExceptionSecondParameterHigherMaxRange.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 6, but there are only 1-5 available.");
    }
}
