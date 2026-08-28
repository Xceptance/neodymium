package org.neodymium.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

import org.neodymium.util.NeodymiumAnnotationUtils;

public abstract class Data
{
    public static <T extends Annotation> List<T> getAnnotations(AnnotatedElement object, Class<T> annotationClass)
    {
        return NeodymiumAnnotationUtils.getAnnotations(object, annotationClass);
    }

    public static <T extends Annotation> List<T> getDeclaredAnnotations(AnnotatedElement object, Class<T> annotationClass)
    {
        return NeodymiumAnnotationUtils.getAnnotations(object, annotationClass);
    }
}

