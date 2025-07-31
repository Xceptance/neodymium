package com.xceptance.neodymium.common.retry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.xceptance.neodymium.common.Data;

public class RetryData
{
    public List<String> getExceptions()
    {
        return exceptions;
    }

    public int getMaxExecutions()
    {
        return maxExecutions;
    }

    private List<String> exceptions;

    private int maxExecutions;

    public RetryData(Method templateMethod)
    {
        var testClassRetryAnnotations = Data.getDeclaredAnnotations(templateMethod.getDeclaringClass(), Retry.class);
        var methodRetryAnnotations = Data.getAnnotations(templateMethod, Retry.class);

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

        maxExecutions = !methodRetryAnnotations.isEmpty() ? methodRetryAnnotations.get(0).value()
                                                          : !testClassRetryAnnotations.isEmpty() ? testClassRetryAnnotations.get(0).value() : 1;
    }

    public List<RetryMethodData> createIterationData()
    {
        List<RetryMethodData> iterationData = new ArrayList<RetryMethodData>();
        for (int i = 0; i < maxExecutions; i++)
        {
            iterationData.add(new RetryMethodData(new ArrayList<String>(exceptions), maxExecutions, i));
        }
        return iterationData;
    }
}
