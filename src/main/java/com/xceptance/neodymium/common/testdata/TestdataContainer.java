package com.xceptance.neodymium.common.testdata;

/**
 * @deprecated Use {@link org.neodymium.common.testdata.TestdataContainer} instead.
 */
@Deprecated
public class TestdataContainer extends org.neodymium.common.testdata.TestdataContainer
{
    public TestdataContainer(java.util.Map<java.lang.String, java.lang.String> dataSet, java.util.Map<java.lang.String, java.lang.String> packageTestData, int index, int size)
    {
        super(dataSet, packageTestData, index, size);
    }

    public TestdataContainer(org.neodymium.common.testdata.TestdataContainer another)
    {
        super(another);
    }
}
