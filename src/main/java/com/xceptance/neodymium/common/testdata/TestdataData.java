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

    private List<TestdataContainer> avaliableDataSets;

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
        avaliableDataSets = iterations;
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
            fixedIterations.addAll(avaliableDataSets);
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
        List<TestdataContainer> iterations = new LinkedList<>();
        for (DataSet dataSetAnnotation : dataSetAnnotationFilter)
        {
            int dataSetIndex = dataSetAnnotation.value();
            String dataSetId = dataSetAnnotation.id();
            // take dataSetId (testId) if its set
            if (dataSetId != null && dataSetId.trim().length() > 0)
            {
                List<TestdataContainer> foundDataSet = avaliableDataSets.stream().filter(dataSet -> dataSet.getDataSet().get(TEST_ID).equals(dataSetId))
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
                // use index
                if (dataSetIndex <= 0)
                {
                    // add all data sets
                    iterations.addAll(avaliableDataSets);
                }
                else
                {
                    if (dataSetIndex > avaliableDataSets.size())
                    {
                        String msg = MessageFormat.format("Method ''{0}'' is marked to be run with data set index {1}, but there are only {2} available",
                                                          method.getName(), dataSetIndex, iterations.size());
                        throw new IllegalArgumentException(msg);
                    }
                    else
                    {
                        iterations.add(avaliableDataSets.get(dataSetIndex - 1));
                    }
                }
            }
        }

        return processDuplicates(iterations);
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
