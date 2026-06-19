package com.xceptance.neodymium.junit4.statement.testdata;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
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
    public String getCategoryName(final Object data)
    {
        final TestdataContainer parameter = (TestdataContainer) data;
        final Map<String, String> testData = new HashMap<>();
        testData.putAll(parameter.getPackageTestData());
        testData.putAll(parameter.getDataSet());

        if (parameter.getIndex() >= 0)
        {
            String testDatasetId = testData.get(TEST_ID);
            if (StringUtils.isBlank(testDatasetId))
            {
                testDatasetId = testData.get("TEST_ID");
            }

            if (StringUtils.isNotBlank(testDatasetId))
            {
                return testDatasetId.replaceAll("\\(", "[").replaceAll("\\)", "]");
            }
            else
            {
                final String sourceFile = testData.get("neodymium.sourceFile");
                if (parameter.isFromDataFolder() && StringUtils.isNotBlank(sourceFile))
                {
                    final String baseName = FilenameUtils.getBaseName(sourceFile);
                    return String.format("%s - Data set %d / %d", baseName, (parameter.getIndex() + 1), parameter.getSize());
                }
                else
                {
                    return String.format("Data set %d / %d", (parameter.getIndex() + 1), parameter.getSize());
                }
            }
        }
        else
        {
            return "TestData";
        }
    }
}
