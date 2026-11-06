package com.xceptance.neodymium.junit5.testdata;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.xceptance.neodymium.common.testdata.TestdataContainer;
import com.xceptance.neodymium.common.testdata.TestdataRunner;

public class TestdataCallback implements BeforeEachCallback
{
    private TestdataRunner testdataRunner;

    public TestdataCallback(TestdataContainer testData, Object testClassInstance)
    {
        testdataRunner = new TestdataRunner(testData);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception
    {
        testdataRunner.setUpTest(context.getRequiredTestInstance());
    }

}
