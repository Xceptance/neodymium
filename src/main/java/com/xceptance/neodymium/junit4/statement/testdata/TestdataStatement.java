package com.xceptance.neodymium.junit4.statement.testdata;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.common.testdata.TestdataContainer;
import com.xceptance.neodymium.common.testdata.TestdataData;
import com.xceptance.neodymium.common.testdata.TestdataRunner;
import com.xceptance.neodymium.junit4.StatementBuilder;

public class TestdataStatement extends StatementBuilder<TestdataContainer>
{
    private static final String TEST_ID = "testId";

    public static Logger LOGGER = LoggerFactory.getLogger(TestdataStatement.class);

    private Statement next;

    private TestdataData testdataData;

    private TestdataRunner testdataRunner;

    private Object testClassInstance;

    public TestdataStatement(Statement next, Object parameter, Object testClassInstance)
    {
        this.next = next;
        this.testClassInstance = testClassInstance;
        testdataRunner = new TestdataRunner((TestdataContainer) parameter);
    }

    public TestdataStatement()
    {
    }

    @Override
    public void evaluate() throws Throwable
    {
        testdataRunner.setUpTest(testClassInstance);
        next.evaluate();
    }

    @Override
    public List<TestdataContainer> createIterationData(TestClass testClass, FrameworkMethod method)
    {
        testdataData = new TestdataData(testClass.getJavaClass());
        return testdataData.getTestDataForMethod(method.getMethod());
    }

    @Override
    public TestdataStatement createStatement(Object testClassInstance, Statement next, Object parameter)
    {
        return new TestdataStatement(next, parameter, testClassInstance);
    }

    @Override
    public String getTestName(Object data)
    {
        String testname = getCategoryName(data);
        TestdataContainer parameter = (TestdataContainer) data;

        if (parameter.getIterationIndex() > 0)
        {
            testname += MessageFormat.format(", run #{0}", parameter.getIterationIndex());
        }

        return testname;
    }

    @Override
    public String getCategoryName(Object data)
    {
        TestdataContainer parameter = (TestdataContainer) data;
        Map<String, String> testData = new HashMap<>();
        testData.putAll(parameter.getPackageTestData());
        testData.putAll(parameter.getDataSet());

        String testname;
        if (parameter.getIndex() >= 0)
        {
            // data sets and (maybe) package data
            String testDatasetId = testData.get(TEST_ID);
            if (StringUtils.isBlank(testDatasetId))
            {
                testDatasetId = "Data set";
            }

            // replace parenthesis because https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512
            // "Any text in parentheses is assumed to be the name of the class in which the test is defined."
            testDatasetId = testDatasetId.replaceAll("\\(", "[").replaceAll("\\)", "]");

            testname = String.format("%s %d / %d", testDatasetId, (parameter.getIndex() + 1), parameter.getSize());
        }
        else
        {
            // only package data
            testname = "TestData";
        }
        return testname;
    }
}
