/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
