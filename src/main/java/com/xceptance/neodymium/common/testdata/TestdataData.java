package com.xceptance.neodymium.common.testdata;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.common.testdata.util.TestDataUtils;
import com.xceptance.neodymium.util.Neodymium;

public class TestdataData extends Data
{
    private static final String TEST_ID = "testId";

    private List<TestdataContainer> availableDataSets;

    private List<DataSet> classDataSetAnnotations;

    private RandomDataSets classRandomDataSetAnnotation;

    private SuppressDataSets classSuppressDataSetAnnotation;

    public TestdataData(Class<?> testClass)
    {
        List<Map<String, String>> dataSets;
        Map<String, String> packageTestData;
        try
        {
            dataSets = TestDataUtils.getDataSets(testClass);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        packageTestData = TestDataUtils.getPackageTestData(testClass);

        List<TestdataContainer> iterations = new LinkedList<>();
        if (!dataSets.isEmpty() || !packageTestData.isEmpty())
        {
            if (!dataSets.isEmpty())
            {
                // data sets found
                for (int i = 0; i < dataSets.size(); i++)
                {
                    iterations.add(new TestdataContainer(dataSets.get(i), packageTestData, i, dataSets.size()));
                }
            }
            else
            {
                // only package data, no data sets
                iterations.add(new TestdataContainer(new HashMap<>(), packageTestData, -1, -1));
            }
        }
        else
        {
            // we couldn't find any data sets
        }
        availableDataSets = iterations;
        classDataSetAnnotations = getAnnotations(testClass, DataSet.class);
        classRandomDataSetAnnotation = testClass.getAnnotation(RandomDataSets.class);
        classSuppressDataSetAnnotation = testClass.getAnnotation(SuppressDataSets.class);
    }

    public List<TestdataContainer> getTestDataForMethod(Method testMethod)
    {
        List<DataSet> methodDataSetAnnotation = getAnnotations(testMethod, DataSet.class);
        RandomDataSets methodRandomDataSetAnnotation = testMethod.getAnnotation(RandomDataSets.class);
        SuppressDataSets methodSuppressDataSetAnnotation = testMethod.getAnnotation(SuppressDataSets.class);
        if (methodSuppressDataSetAnnotation != null)
        {
            return new LinkedList<>();
        }
        if (methodDataSetAnnotation.isEmpty() && classSuppressDataSetAnnotation != null)
        {
            // class is marked to suppress data sets and there is no overriding DataSet on the method
            return new LinkedList<>();
        }
        List<TestdataContainer> fixedIterations = new LinkedList<>();
        if (!methodDataSetAnnotation.isEmpty())
        {
            fixedIterations = applyAnnotationFilter(methodDataSetAnnotation, testMethod);
        }
        else if (!classDataSetAnnotations.isEmpty())
        {
            fixedIterations = applyAnnotationFilter(classDataSetAnnotations, testMethod);
        }
        else
        {
            fixedIterations.addAll(availableDataSets);
        }

        // get the desired number of data sets (highest priority on method level)
        int randomSetAmount = methodRandomDataSetAnnotation != null ? methodRandomDataSetAnnotation.value()
                                                                    : classRandomDataSetAnnotation != null ? classRandomDataSetAnnotation.value() : 0;

        // if the amount is < 1 this annotation has no effect at all
        if (randomSetAmount > 0)
        {
            // make sure that not more data sets than available are taken
            if (randomSetAmount > fixedIterations.size())
            {
                String msg = MessageFormat.format("Method ''{0}'' is marked to be run with {1} random data sets, but there are only {2} available",
                                                  testMethod.getName(), randomSetAmount, fixedIterations.size());
                throw new IllegalArgumentException(msg);
            }
            // shuffle the order of the data sets first
            Collections.shuffle(fixedIterations, Neodymium.getRandom());
            // choose the random data sets [0,randomSetAmount[
            fixedIterations = fixedIterations.subList(0, randomSetAmount);
        }
        return fixedIterations;
    }

    private List<TestdataContainer> applyAnnotationFilter(List<DataSet> dataSetAnnotationFilter, Method method)
    {
      if (availableDataSets.size() == 0) 
      {
          throw new IllegalArgumentException("No data sets were found at all regarding your test case, please make sure to reference everything correctly.");
      }
      
        List<TestdataContainer> iterations = new LinkedList<>();
        for (DataSet dataSetAnnotation : dataSetAnnotationFilter)
        {
            int[] dataSetIndex = dataSetAnnotation.value();
            String dataSetId = dataSetAnnotation.id();
            // take dataSetId (testId) if its set
            if (dataSetId != null && dataSetId.trim().length() > 0)
            {
                List<TestdataContainer> foundDataSet = availableDataSets.stream().filter(dataSet -> dataSet.getDataSet().get(TEST_ID).equals(dataSetId))
                                                                        .collect(Collectors.toList());
                if (foundDataSet.isEmpty())
                {
                    String msg = MessageFormat.format("Method ''{0}'' is marked to be run with data set testId ''{1}'', but could not find that data set",
                                                      method.getName(), dataSetId);
                    throw new IllegalArgumentException(msg);
                }
                iterations.add(foundDataSet.get(0));
            }
            else
            {
                if (dataSetIndex.length == 0)
                {
                    iterations.addAll(availableDataSets);
                }
                // make sure to only use 1 or 2 parameters > 0 for DataSet
                else if (dataSetIndex.length > 2)
                {
                    throw new IllegalArgumentException("Only a range of 1-2 parameters are permitted using the DataSet annotation, please adjust your DataSet annotation accordingly.");
                }
                else
                {
                    // make sure to add the specified single data set
                    if (dataSetIndex.length == 1) 
                    {
                        checkIfDataSetIndexIsOutOfBounds(dataSetIndex[0], method);
                        iterations.add(availableDataSets.get(dataSetIndex[0] - 1));
                    }
                    // make sure to add the specified data set range
                    else 
                    {
                        checkIfDataSetIndexIsOutOfBounds(dataSetIndex[0], method);
                        checkIfDataSetIndexIsOutOfBounds(dataSetIndex[1], method);
                        if (dataSetIndex[0] > dataSetIndex[1] == false) 
                        {
                            for (int i = dataSetIndex[0]; i <= dataSetIndex[1]; i++) 
                            {
                                iterations.add(availableDataSets.get(i - 1));
                            }
                        }
                        else 
                        {
                            String msg = MessageFormat.format("The minimum value ''{0}'' happens to be greater than the maximum value ''{1}'', please adjust.",
                                                              dataSetIndex[0], dataSetIndex[1]);
                            throw new IllegalArgumentException(msg);
                        }
                    }
                }
            }
        }

        return processDuplicates(iterations);
    }
    
    private void checkIfDataSetIndexIsOutOfBounds(int dataSetIndex, Method method) 
    {
        if (dataSetIndex <= 0 || dataSetIndex > availableDataSets.size())
        {
            String msg = MessageFormat.format("Method ''{0}'' is marked to be run with data set index {1}, but there are only {2} available.",
                                              method.getName(), dataSetIndex, availableDataSets.size());
            throw new IllegalArgumentException(msg);
        }
    }

    private List<TestdataContainer> processDuplicates(List<TestdataContainer> iterations)
    {
        // since the user can decide to annotate the same data set several times to the same function we need to care
        // about the duplicates. First of all we need to clone those objects, then we need to set a special index which
        // will be later used to distinguish them in the run

        // this map contains the counter for the new index
        HashMap<TestdataContainer, Integer> iterationIndexMap = new HashMap<>();
        List<TestdataContainer> fixedIterations = new LinkedList<>();

        for (TestdataContainer testdataContainer : iterations)
        {
            if (!fixedIterations.contains(testdataContainer))
            {
                // no duplicate, just add it
                fixedIterations.add(testdataContainer);
            }
            else
            {
                // now the funny part, we encountered an duplicated object

                // always set the first occurrence of an object to 1
                testdataContainer.setIterationIndex(1);

                // set the counter for this object to 1
                iterationIndexMap.computeIfAbsent(testdataContainer, (o) -> {
                    return 1;
                });

                // increment the counter every time we visit with the same object
                Integer newIndex = iterationIndexMap.computeIfPresent(testdataContainer, (o, index) -> {
                    return (index + 1);
                });

                // important: we clone that object
                TestdataContainer clonedObject = new TestdataContainer(testdataContainer);
                // set the "iteration" index to the new cloned object
                clonedObject.setIterationIndex(newIndex);
                // add it to the list
                fixedIterations.add(clonedObject);
            }
        }

        return fixedIterations;
    }
}
