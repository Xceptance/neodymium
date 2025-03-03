package com.xceptance.neodymium.junit4;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

public abstract class StatementBuilder<T> extends Statement
{
    /**
     * Create iteration data for the test method in tests class
     * 
     * @param testClass
     * @param method
     * @return
     * @throws Throwable
     */
    public abstract List<T> createIterationData(TestClass testClass, FrameworkMethod method) throws Throwable;

    /**
     * Create statement for the test class
     * 
     * @param testClassInstance
     * @param next
     * @param parameter
     * @return
     */
    public abstract StatementBuilder<T> createStatement(Object testClassInstance, Statement next, Object parameter);

    /**
     * Get name of the test for which statement is created
     * 
     * @param data
     * @return
     */
    public abstract String getTestName(Object data);

    /**
     * Get name of the category of the test method
     * 
     * @param data
     * @return
     */
    public abstract String getCategoryName(Object data);

    public static <R extends StatementBuilder<?>> R instantiate(Class<R> clazz)
    {
        try
        {
            return clazz.getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> getDeclaredAnnotations(AnnotatedElement object, Class<T> annotationClass)
    {
        List<T> annotations = new LinkedList<>();
        if (object == null || annotationClass == null)
        {
            return annotations;
        }

        // check if the annotation is repeatable
        Repeatable repeatingAnnotation = annotationClass.getAnnotation(Repeatable.class);
        Annotation annotation = (repeatingAnnotation == null) ? null : object.getDeclaredAnnotation(repeatingAnnotation.value());

        if (annotation != null)
        {
            try
            {
                annotations.addAll(Arrays.asList((T[]) annotation.getClass().getMethod("value").invoke(annotation)));
            }
            catch (ReflectiveOperationException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            T anno = object.getDeclaredAnnotation(annotationClass);
            if (anno != null)
            {
                annotations.add(anno);
            }
        }

        return annotations;
    }
}
