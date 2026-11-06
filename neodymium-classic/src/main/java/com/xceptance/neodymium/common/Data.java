package com.xceptance.neodymium.common;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class Data
{
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> getAnnotations(AnnotatedElement object, Class<T> annotationClass)
    {
        List<T> annotations = new LinkedList<>();
        if (object == null || annotationClass == null)
        {
            return annotations;
        }

        // check if the annotation is repeatable
        Repeatable repeatingAnnotation = annotationClass.getAnnotation(Repeatable.class);
        Annotation annotation = (repeatingAnnotation == null) ? null : object.getAnnotation(repeatingAnnotation.value());

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
            T anno = object.getAnnotation(annotationClass);
            if (anno != null)
            {
                annotations.add(anno);
            }
        }

        return annotations;
    }
}
