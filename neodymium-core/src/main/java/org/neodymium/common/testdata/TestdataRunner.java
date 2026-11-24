/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common.testdata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestdataRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestdataRunner.class);

    private TestdataContainer testData;

    public TestdataRunner(TestdataContainer testData)
    {
        this.testData = testData;
    }

    public void setUpTest(final Object testClassInstance) throws IllegalArgumentException, IllegalAccessException
    {
        if (testData != null)
        {
            final Map<String, String> dataSet = testData.getDataSet();
            final String sourceFile = dataSet.get("neodymium.sourceFile");
            if (sourceFile != null)
            {
                Neodymium.setTestdataSourceFile(sourceFile);
            }
            Neodymium.getData().putAll(dataSet);
            Neodymium.getData().remove("neodymium.sourceFile");
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
            DataItem dataAnnotation = NeodymiumAnnotationUtils.getAnnotations(field, DataItem.class).stream().findFirst().orElse(null);
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
