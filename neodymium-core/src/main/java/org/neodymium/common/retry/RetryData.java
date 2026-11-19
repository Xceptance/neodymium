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
package org.neodymium.common.retry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.neodymium.common.Data;
import org.neodymium.util.NeodymiumAnnotationUtils;

/**
 * Class to store information about maximal number of retries and expected errors for the specific test. Produces
 * {@link RetryMethodData} for every possible iteration of the test
 */
public class RetryData
{
    private List<String> exceptions;

    private int maxExecutions;

    /**
     * Constructs the {@link RetryData} object
     * 
     * @param templateMethod
     *            - test method
     */
    public RetryData(Method templateMethod)
    {
         List<Retry> testClassRetryAnnotations = getDeclaredAnnotations(templateMethod.getDeclaringClass(), Retry.class);
         List<Retry> methodRetryAnnotations = Data.getAnnotations(templateMethod, Retry.class);

        exceptions = new ArrayList<String>();
        if (!testClassRetryAnnotations.isEmpty())
        {
            for (Retry testClassRetryAnnotation : testClassRetryAnnotations)
            {
                if (testClassRetryAnnotation.exceptions().length == 0)
                {
                    exceptions = new ArrayList<String>();
                    break;
                }
                exceptions.addAll(List.of(testClassRetryAnnotation.exceptions()));
            }
        }
        if (!methodRetryAnnotations.isEmpty())
        {
            for (Retry methodRetryAnnotation : methodRetryAnnotations)
            {
                if (methodRetryAnnotation.exceptions().length == 0)
                {
                    exceptions = new ArrayList<String>();
                    break;
                }
                exceptions.addAll(List.of(methodRetryAnnotation.exceptions()));
            }
        }

        maxExecutions = !methodRetryAnnotations.isEmpty() ? methodRetryAnnotations.get(0).maxNumberOfRetries()
                                                          : !testClassRetryAnnotations.isEmpty() ? testClassRetryAnnotations.get(0).maxNumberOfRetries() : 1;
    }

    /**
     * Creates {@link RetryMethodData} for every possible iteration of the method
     * 
     * @return list of iterations represented by {@link RetryMethodData}
     */
    public List<RetryMethodData> createIterationData()
    {
        List<RetryMethodData> iterationData = new ArrayList<RetryMethodData>();
        for (int i = 0; i < maxExecutions; i++)
        {
            iterationData.add(new RetryMethodData(new ArrayList<String>(exceptions), maxExecutions, i));
        }
        return iterationData;
    }

    private <T extends Annotation> List<T> getDeclaredAnnotations(Class<?> type, Class<T> annotationClass)
    {
        List<T> annotations = new ArrayList<T>();
        while (type != null)
        {
            List<T> annotationsOfCurrentType = NeodymiumAnnotationUtils.getAnnotations(type, annotationClass);
            if (!annotationsOfCurrentType.isEmpty())
            {
                annotations.addAll(annotationsOfCurrentType);
            }
            type = type.getSuperclass();
        }
        return annotations;
    }
}
