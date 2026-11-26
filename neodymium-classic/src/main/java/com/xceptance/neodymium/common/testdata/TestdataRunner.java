/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.common.testdata;

import com.xceptance.neodymium.util.Neodymium;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                        field.set(testClassInstance, Neodymium.getData().get(dataAnnotation.value(), field.getType()));
                    }
                    else if (Neodymium.getData().exists(field.getName()))
                    {
                        field.set(testClassInstance, Neodymium.getData().get("$." + field.getName(), field.getType()));
                    }
                    else if (Neodymium.getData().getDataAsJsonObject().isJsonPrimitive() == (field.getType().isPrimitive() || field.getType()
                                                                                                                                   .equals(String.class)))
                    {
                        field.set(testClassInstance, Neodymium.getData().get(field.getType()));
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
