package com.xceptance.neodymium.common.testdata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.util.DataUtils;
import com.xceptance.neodymium.util.Neodymium;

public class TestdataRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestdataRunner.class);

    private TestdataContainer testData;

    public TestdataRunner(TestdataContainer testData)
    {
        this.testData = testData;
    }

    public void setUpTest(Object testClassInstance) throws IllegalArgumentException, IllegalAccessException
    {
        if (testData != null)
        {
            Neodymium.getData().putAll(testData.getDataSet());
            initializeDataObjects(testClassInstance);
        }
        else
        {
            LOGGER.debug("using no dataset");
        }
    }

    private void initializeDataObjects(Object testClassInstance) throws IllegalArgumentException, IllegalAccessException
    {
        for (Field field : getFieldsFromSuperclasses(testClassInstance))
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

    private List<Field> getFieldsFromSuperclasses(Object testClassInstance)
    {
        Class<?> currentSuperclass = testClassInstance.getClass().getSuperclass();
        ArrayList<Field> fields = new ArrayList<Field>(Arrays.asList(testClassInstance.getClass().getDeclaredFields()));
        while (!currentSuperclass.equals(Object.class))
        {
            fields.addAll(List.of(currentSuperclass.getDeclaredFields()));
            currentSuperclass = currentSuperclass.getSuperclass();
        }
        return fields;
    }
}
