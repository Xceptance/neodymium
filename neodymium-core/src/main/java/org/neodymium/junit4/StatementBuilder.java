package org.neodymium.junit4;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.neodymium.util.NeodymiumAnnotationUtils;
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
        return NeodymiumAnnotationUtils.getAnnotations(object, annotationClass);
    }
}
