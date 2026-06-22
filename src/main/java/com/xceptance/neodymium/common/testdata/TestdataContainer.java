package com.xceptance.neodymium.common.testdata;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class TestdataContainer
{

    private Map<String, String> dataSet;

    private Map<String, String> packageTestData;

    private int index;

    private int size;

    private int iterationIndex;

    private boolean fromDataFolder = false;

    public TestdataContainer(Map<String, String> dataSet, Map<String, String> packageTestData, int index, int size)
    {
        this.dataSet = dataSet;
        this.packageTestData = packageTestData;
        this.index = index;
        this.size = size;
        this.iterationIndex = 0;
    }

    public TestdataContainer(TestdataContainer another)
    {
        if (another.dataSet != null)
        {
            this.dataSet = new HashMap<>();
            this.dataSet.putAll(another.dataSet);
        }

        if (another.packageTestData != null)
        {
            this.packageTestData = new HashMap<>();
            this.packageTestData.putAll(another.packageTestData);
        }
        this.size = another.size;
        this.index = another.index;
        this.iterationIndex = another.iterationIndex;
        this.fromDataFolder = another.fromDataFolder;
    }

    public Map<String, String> getDataSet()
    {
        Map<String, String> fullDataSet = new HashMap<>();
        fullDataSet.putAll(packageTestData);
        fullDataSet.putAll(dataSet);
        return fullDataSet;
    }

    public Map<String, String> getPackageTestData()
    {
        return packageTestData;
    }

    public int getIndex()
    {
        return index;
    }

    public int getSize()
    {
        return size;
    }

    public int getIterationIndex()
    {
        return iterationIndex;
    }

    public void setIterationIndex(int iterationIndex)
    {
        this.iterationIndex = iterationIndex;
    }

    public final boolean isFromDataFolder()
    {
        return fromDataFolder;
    }

    public final void setFromDataFolder(final boolean fromDataFolder)
    {
        this.fromDataFolder = fromDataFolder;
    }

    public String getTitle()
    {
        final String dataSetId = dataSet.get("testId") != null ? dataSet.get("testId") : dataSet.get("TEST_ID");
        final String sourceFile = dataSet.get("neodymium.sourceFile");
        String baseName = null;
        if (fromDataFolder && sourceFile != null && !sourceFile.trim().isEmpty())
        {
            baseName = FilenameUtils.getBaseName(sourceFile);
        }

        final String mainTitle;
        if (dataSet.isEmpty() && !packageTestData.isEmpty())
        {
            mainTitle = " :: TestData";
        }
        else if (dataSetId != null)
        {
            mainTitle = " :: " + dataSetId;
        }
        else if (size > -1)
        {
            if (baseName != null)
            {
                mainTitle = " :: " + baseName + " - Data set " + (index + 1) + " / " + size;
            }
            else
            {
                mainTitle = " :: Data set " + (index + 1) + " / " + size;
            }
        }
        else
        {
            mainTitle = "";
        }
        return mainTitle + (getIterationIndex() > 0 ? ", run #" + getIterationIndex() : "");
    }
}
