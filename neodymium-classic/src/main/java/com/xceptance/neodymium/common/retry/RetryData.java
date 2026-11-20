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
package com.xceptance.neodymium.common.retry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.xceptance.neodymium.common.Data;

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
            T annotationsOfCurrentType = type.getDeclaredAnnotation(annotationClass);
            if (annotationsOfCurrentType != null)
            {
                annotations.addAll(List.of(annotationsOfCurrentType));
            }
            type = type.getSuperclass();
        }
        return annotations;
    }
}
