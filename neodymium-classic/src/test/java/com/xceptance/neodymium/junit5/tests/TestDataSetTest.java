/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateAllDataSets;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionFirstParameterHigherMaxRange;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionFirstParameterLowerMinRange;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionMoreThanTwoParameters;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionNoDataSetFound;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionOnlyParameterHigherMaxRange;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionOnlyParameterLowerMinRange;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionSecondParameterHigherMaxRange;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateDataSetExceptionSecondParameterLowerMinRange;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateMultipleDataSets;
import com.xceptance.neodymium.junit5.testclasses.data.annotation.InstantiateSingleDataSet;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class TestDataSetTest extends AbstractNeodymiumTest
{
    @Test
    public void testNoDataSetsFound()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionNoDataSetFound.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: No data sets were found at all regarding your test case, please make sure to reference everything correctly.");
    }
    
    @Test
    public void testInstantiateAllDataSets()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateAllDataSets.class);
        checkPass(summary, 10, 0);
    }
    
    @Test
    public void testInstantiateSingleDataSet()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateSingleDataSet.class);
        checkPass(summary, 10, 0);
    }
    
    @Test
    public void testInstantiateMultipleDataSets()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateMultipleDataSets.class);
        checkPass(summary, 10, 0);
    }
    
    @Test
    public void testDataSetExceptionMoreThanTwoParameters()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionMoreThanTwoParameters.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Only a range of 1-2 parameters are permitted using the DataSet annotation, please adjust your DataSet annotation accordingly.");
    }
    
    @Test
    public void testDataSetExceptionOnlyParameterLowerMinRange()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionOnlyParameterLowerMinRange.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 0, but there are only 1-5 available.");
    }
    
    @Test
    public void testDataSetExceptionOnlyParameterHigherMaxRange()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionOnlyParameterHigherMaxRange.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 6, but there are only 1-5 available.");
    }
    
    @Test
    public void testDataSetExceptionFirstParameterLowerMinRange()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionFirstParameterLowerMinRange.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 0, but there are only 1-5 available.");
    }
    
    @Test
    public void testDataSetExceptionFirstParameterHigherMaxRange()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionFirstParameterHigherMaxRange.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 6, but there are only 1-5 available.");
    }
    
    @Test
    public void testDataSetExceptionSecondParameterLowerMinRange()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionSecondParameterLowerMinRange.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 0, but there are only 1-5 available.");
    }
    
    @Test
    public void testDataSetExceptionSecondParameterHigherMaxRange()
    {
        NeodymiumTestExecutionSummary summary = run(InstantiateDataSetExceptionSecondParameterHigherMaxRange.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with data set index 6, but there are only 1-5 available.");
    }
}
