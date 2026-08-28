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
