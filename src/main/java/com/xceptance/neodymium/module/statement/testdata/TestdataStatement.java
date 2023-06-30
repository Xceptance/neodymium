package com.xceptance.neodymium.module.statement.testdata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.module.StatementBuilder;
import com.xceptance.neodymium.module.statement.testdata.util.TestDataUtils;
import com.xceptance.neodymium.util.DataUtils;
import com.xceptance.neodymium.util.Neodymium;

/**
 * JUnit statement to read the test data and multiply tests by data sets
 */
public class TestdataStatement extends StatementBuilder
{
    private static final String TEST_ID = "testId";

    public static Logger LOGGER = LoggerFactory.getLogger(TestdataStatement.class);

    private Statement next;

    private Object testClassInstance;

    Map<String, String> testData;

    public TestdataStatement(Statement next, TestdataStatementData parameter, Object testClassInstance)
    {
        this.next = next;
        this.testClassInstance = testClassInstance;
        int currentDataSetIndex = parameter.getIndex();

        testData = new HashMap<>();
        testData.putAll(parameter.getPackageTestData());

        if (currentDataSetIndex >= 0)
        {
            for (Entry<String, String> newDataEntry : parameter.getDataSet().entrySet())
            {
                // only log if a data set entry overwrites an package data entry
                if (testData.containsKey(newDataEntry.getKey()))
                {
                    LOGGER.debug(String.format("Data entry \"%s\" overwritten by data set #%d (old: \"%s\", new: \"%s\")",
                                               newDataEntry.getKey(), currentDataSetIndex + 1, testData.get(newDataEntry.getKey()),
                                               newDataEntry.getValue()));
                }
                testData.put(newDataEntry.getKey(), newDataEntry.getValue());
            }
        }
    }

    public TestdataStatement()
    {
    }

    @Override
    public void evaluate() throws Throwable
    {
        Neodymium.getData().putAll(testData);
        initializeDataObjects();
        next.evaluate();
    }

    private void initializeDataObjects() throws IllegalArgumentException, IllegalAccessException
    {
        for (Field field : getFieldsFromSuperclasses())
        {
            DataItem dataAnnotation = field.getAnnotation(DataItem.class);
            if (dataAnnotation != null)
            {
                boolean isFieldAccessable = field.canAccess(testClassInstance);
                field.setAccessible(true);
                try
                {
                    if (!StringUtils.isBlank(dataAnnotation.value()))
                    {
                        field.set(testClassInstance, DataUtils.get(dataAnnotation.value(), field.getType()));
                    }
                    else if (DataUtils.exists(field.getName()))
                    {
                        field.set(testClassInstance, DataUtils.get("$." + field.getName(), field.getType()));
                    }
                    else if (DataUtils.getDataAsJsonObject().isJsonPrimitive() == (field.getType().isPrimitive() || field.getType().equals(String.class)))
                    {
                        field.set(testClassInstance, DataUtils.get(field.getType()));
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Something went wrong while test data value injection for field:'" + field.getName() + "' in class:'"
                                               + testClassInstance.getClass().getName() + "'", e);
                }
                finally
                {
                    field.setAccessible(isFieldAccessable);
                }
            }
        }
    }

    private List<Field> getFieldsFromSuperclasses()
    {
        var currentSuperclass = testClassInstance.getClass().getSuperclass();
        var fields = new ArrayList<Field>(Arrays.asList(testClassInstance.getClass().getDeclaredFields()));
        while (!currentSuperclass.equals(Object.class))
        {
            fields.addAll(List.of(currentSuperclass.getDeclaredFields()));
            currentSuperclass = currentSuperclass.getSuperclass();
        }
        return fields;
    }

    @Override
    public List<Object> createIterationData(TestClass testClass, FrameworkMethod method)
    {
        List<Map<String, String>> dataSets;
        Map<String, String> packageTestData;
        try
        {
            dataSets = TestDataUtils.getDataSets(testClass.getJavaClass());
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        packageTestData = TestDataUtils.getPackageTestData(testClass.getJavaClass());

        List<Object> iterations = new LinkedList<>();
        if (!dataSets.isEmpty() || !packageTestData.isEmpty())
        {
            if (!dataSets.isEmpty())
            {
                // data sets found
                for (int i = 0; i < dataSets.size(); i++)
                {
                    iterations.add(new TestdataStatementData(dataSets.get(i), packageTestData, i, dataSets.size()));
                }
            }
            else
            {
                // only package data, no data sets
                iterations.add(new TestdataStatementData(new HashMap<>(), packageTestData, -1, -1));
            }
        }
        else
        {
            // we couldn't find any data sets
        }
        iterations = processOverrides(testClass, method, iterations);
        iterations = processDuplicates(iterations);

        return iterations;
    }

    private List<Object> processDuplicates(List<Object> iterations)
    {
        // since the user can decide to annotate the same data set several times to the same function we need to care
        // about the duplicates. First of all we need to clone those objects, then we need to set a special index which
        // will be later used to distinguish them in the run

        // this map contains the counter for the new index
        HashMap<Object, Integer> iterationIndexMap = new HashMap<>();
        List<Object> fixedIterations = new LinkedList<>();

        for (Object object : iterations)
        {
            if (!fixedIterations.contains(object))
            {
                // no duplicate, just add it
                fixedIterations.add(object);
            }
            else
            {
                // now the funny part, we encountered an duplicated object

                // always set the first occurrence of an object to 1
                TestdataStatementData existingObject = (TestdataStatementData) object;
                existingObject.setIterationIndex(1);

                // set the counter for this object to 1
                iterationIndexMap.computeIfAbsent(object, (o) -> {
                    return 1;
                });

                // increment the counter every time we visit with the same object
                Integer newIndex = iterationIndexMap.computeIfPresent(object, (o, index) -> {
                    return (index + 1);
                });

                // important: we clone that object
                TestdataStatementData clonedObject = new TestdataStatementData((TestdataStatementData) object);
                // set the "iteration" index to the new cloned object
                clonedObject.setIterationIndex(newIndex);
                // add it to the list
                fixedIterations.add(clonedObject);
            }
        }

        return fixedIterations;
    }

    private List<Object> processOverrides(TestClass testClass, FrameworkMethod method, List<Object> iterations)
    {
        SuppressDataSets methodSuppress = method.getAnnotation(SuppressDataSets.class);
        if (methodSuppress != null)
        {
            // the test method is marked to suppress data sets
            return new LinkedList<>();
        }

        SuppressDataSets classSuppress = testClass.getAnnotation(SuppressDataSets.class);
        List<DataSet> methodDataSetAnnotations = getAnnotations(method.getMethod(), DataSet.class);

        if (methodDataSetAnnotations.isEmpty() && classSuppress != null)
        {
            // class is marked to suppress data sets and there is no overriding DataSet on the method
            return new LinkedList<>();
        }

        List<DataSet> dataSetAnnotations = new LinkedList<>();

        // at this point neither the class nor the method could have data sets suppressed
        List<DataSet> classDataSetAnnotations = getAnnotations(testClass.getJavaClass(), DataSet.class);
        if (!methodDataSetAnnotations.isEmpty())
        {
            dataSetAnnotations = methodDataSetAnnotations;
        }
        else if (!classDataSetAnnotations.isEmpty())
        {
            dataSetAnnotations = classDataSetAnnotations;
        }

        List<Object> fixedIterations = new LinkedList<>();
        if (dataSetAnnotations.isEmpty())
        {
            // so there is nothing to suppress and nothing to override. Go ahead with all data sets
            fixedIterations.addAll(iterations);
        }
        for (DataSet dataSet : dataSetAnnotations)
        {
            int dataSetIndex = dataSet.value();
            String dataSetId = dataSet.id();

            // take dataSetId (testId) if its set
            if (dataSetId != null && dataSetId.trim().length() > 0)
            {
                // search the dataset
                boolean found = false;
                for (Object object : iterations)
                {
                    TestdataStatementData o = (TestdataStatementData) object;
                    String testId = o.getDataSet().get(TEST_ID);
                    if (dataSetId.equals(testId))
                    {
                        fixedIterations.add(object);
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    String msg = MessageFormat.format("Method ''{0}'' is marked to be run with data set testId ''{1}'', but could not find that data set",
                                                      method.getName(), dataSetId);
                    throw new IllegalArgumentException(msg);
                }
            }
            else
            {
                // use index
                if (dataSetIndex <= 0)
                {
                    // add all data sets
                    fixedIterations.addAll(iterations);
                }
                else
                {
                    if (dataSetIndex > iterations.size())
                    {
                        String msg = MessageFormat.format("Method ''{0}'' is marked to be run with data set index {1}, but there are only {2} available",
                                                          method.getName(), dataSetIndex, iterations.size());
                        throw new IllegalArgumentException(msg);
                    }
                    else
                    {
                        fixedIterations.add(iterations.get(dataSetIndex - 1));
                    }
                }
            }
        }

        // check if the test is annotated with @RandomDataSets and if so, randomize the previously refined list of data
        // sets and select the desired number of data sets
        RandomDataSets methodRandomDataSetsAnnotation = method.getAnnotation(RandomDataSets.class);
        RandomDataSets classRandomDataSetsAnnotation = testClass.getAnnotation(RandomDataSets.class);

        // get the desired number of data sets (highest priority on method level)
        int randomSetAmount = methodRandomDataSetsAnnotation != null ? methodRandomDataSetsAnnotation.value()
                                                                     : classRandomDataSetsAnnotation != null ? classRandomDataSetsAnnotation.value() : 0;

        // if the amount is < 1 this annotation has no effect at all
        if (randomSetAmount > 0)
        {
            // make sure that not more data sets than available are taken
            if (randomSetAmount > fixedIterations.size())
            {
                String msg = MessageFormat.format("Method ''{0}'' is marked to be run with {1} random data sets, but there are only {2} available",
                                                  method.getName(), randomSetAmount, iterations.size());
                throw new IllegalArgumentException(msg);
            }
            // shuffle the order of the data sets first
            Collections.shuffle(fixedIterations, Neodymium.getRandom());
            // choose the random data sets [0,randomSetAmount[
            fixedIterations = fixedIterations.subList(0, randomSetAmount);
        }
        return fixedIterations;
    }

    @Override
    public StatementBuilder createStatement(Object testClassInstance, Statement next, Object parameter)
    {
        return new TestdataStatement(next, (TestdataStatementData) parameter, testClassInstance);
    }

    @Override
    public String getTestName(Object data)
    {
        String testname = getCategoryName(data);
        TestdataStatementData parameter = (TestdataStatementData) data;

        if (parameter.getIterationIndex() > 0)
        {
            testname += MessageFormat.format(", run #{0}", parameter.getIterationIndex());
        }

        return testname;
    }

    @Override
    public String getCategoryName(Object data)
    {
        TestdataStatementData parameter = (TestdataStatementData) data;
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
