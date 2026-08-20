/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to feelings of freedom.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class to resolve annotations for both {@code org.neodymium.*} and legacy {@code com.xceptance.neodymium.*} packages.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAnnotationUtils
{
    private static final ConcurrentMap<Class<?>, Class<?>> COUNTERPART_CACHE = new ConcurrentHashMap<>();

    private NeodymiumAnnotationUtils()
    {
    }

    /**
     * Gets all annotations of the specified type (including repeatable/container instances and legacy package counterparts) on an annotated element.
     *
     * @param <T> the target annotation type
     * @param element the annotated element
     * @param annotationClass the target annotation class
     * @return a list of matching annotation instances or proxies
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> getAnnotations(final AnnotatedElement element, final Class<T> annotationClass)
    {
        if (element == null || annotationClass == null)
        {
            return Collections.emptyList();
        }

        final List<T> result = new ArrayList<>();

        // 1. Direct / Repeatable check for the target annotation class
        collectAnnotationsForType(element, annotationClass, result);

        // 2. Check for legacy/counterpart annotation type if result is still empty
        if (result.isEmpty())
        {
            final Class<?> counterpartClass = getCounterpartClass(annotationClass);
            if (counterpartClass != null && Annotation.class.isAssignableFrom(counterpartClass))
            {
                final List<Annotation> counterpartList = new ArrayList<>();
                collectAnnotationsForTypeUntyped(element, (Class<? extends Annotation>) counterpartClass, counterpartList);

                for (final Annotation counterpartAnno : counterpartList)
                {
                    result.add(createAnnotationProxy(counterpartAnno, annotationClass));
                }
            }
        }

        return result;
    }

    /**
     * Gets a single annotation of the specified type or its legacy counterpart.
     *
     * @param <T> the target annotation type
     * @param element the annotated element
     * @param annotationClass the target annotation class
     * @return the annotation instance/proxy or null if not present
     */
    public static <T extends Annotation> T getAnnotation(final AnnotatedElement element, final Class<T> annotationClass)
    {
        final List<T> list = getAnnotations(element, annotationClass);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Overloaded helper for JUnit 4 FrameworkMethod.
     */
    public static boolean isAnnotationPresent(final org.junit.runners.model.FrameworkMethod frameworkMethod, final Class<? extends Annotation> annotationClass)
    {
        return frameworkMethod != null && isAnnotationPresent(frameworkMethod.getMethod(), annotationClass);
    }

    /**
     * Overloaded helper for JUnit 4 FrameworkMethod.
     */
    public static <T extends Annotation> List<T> getAnnotations(final org.junit.runners.model.FrameworkMethod frameworkMethod, final Class<T> annotationClass)
    {
        return frameworkMethod == null ? Collections.emptyList() : getAnnotations(frameworkMethod.getMethod(), annotationClass);
    }

    /**
     * Checks if either the target annotation type or its legacy counterpart is present on the element.
     *
     * @param element the annotated element
     * @param annotationClass the target annotation class
     * @return true if present, false otherwise
     */
    public static boolean isAnnotationPresent(final AnnotatedElement element, final Class<? extends Annotation> annotationClass)
    {
        if (element == null || annotationClass == null)
        {
            return false;
        }

        if (element.isAnnotationPresent(annotationClass))
        {
            return true;
        }

        final Class<?> counterpartClass = getCounterpartClass(annotationClass);
        if (counterpartClass != null && Annotation.class.isAssignableFrom(counterpartClass))
        {
            final Class<? extends Annotation> counterpartAnnoClass = (Class<? extends Annotation>) counterpartClass;
            if (element.isAnnotationPresent(counterpartAnnoClass))
            {
                return true;
            }

            final Repeatable repeatingAnnotation = counterpartAnnoClass.getAnnotation(Repeatable.class);
            if (repeatingAnnotation != null && element.isAnnotationPresent(repeatingAnnotation.value()))
            {
                return true;
            }
        }

        final Repeatable repeatingAnnotation = annotationClass.getAnnotation(Repeatable.class);
        if (repeatingAnnotation != null && element.isAnnotationPresent(repeatingAnnotation.value()))
        {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> void collectAnnotationsForType(final AnnotatedElement element, final Class<A> type, final List<A> collector)
    {
        final Repeatable repeatingAnnotation = type.getAnnotation(Repeatable.class);
        Annotation containerAnnotation = (repeatingAnnotation == null) ? null : element.getDeclaredAnnotation(repeatingAnnotation.value());
        if (containerAnnotation == null && repeatingAnnotation != null)
        {
            containerAnnotation = element.getAnnotation(repeatingAnnotation.value());
        }

        if (containerAnnotation != null)
        {
            try
            {
                final Method valueMethod = containerAnnotation.getClass().getMethod("value");
                final A[] array = (A[]) valueMethod.invoke(containerAnnotation);
                if (array != null)
                {
                    collector.addAll(Arrays.asList(array));
                }
            }
            catch (final Exception e)
            {
                throw new RuntimeException("Failed to extract repeatable annotations from container", e);
            }
        }
        else
        {
            A anno = element.getDeclaredAnnotation(type);
            if (anno == null)
            {
                anno = element.getAnnotation(type);
            }
            if (anno != null)
            {
                collector.add(anno);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectAnnotationsForTypeUntyped(final AnnotatedElement element, final Class<? extends Annotation> type, final List<Annotation> collector)
    {
        collectAnnotationsForType(element, (Class) type, (List) collector);
    }

    private static Class<?> getCounterpartClass(final Class<?> targetClass)
    {
        return COUNTERPART_CACHE.computeIfAbsent(targetClass, clazz -> {
            final String name = clazz.getName();
            String counterpartName = null;

            if (name.startsWith("org.neodymium."))
            {
                counterpartName = "com.xceptance.neodymium." + name.substring("org.neodymium.".length());
            }
            else if (name.startsWith("com.xceptance.neodymium."))
            {
                counterpartName = "org.neodymium." + name.substring("com.xceptance.neodymium.".length());
            }

            if (counterpartName != null)
            {
                try
                {
                    return Class.forName(counterpartName);
                }
                catch (final ClassNotFoundException e)
                {
                    return Void.TYPE;
                }
            }
            return Void.TYPE;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T createAnnotationProxy(final Annotation source, final Class<T> targetInterface)
    {
        final InvocationHandler handler = new InvocationHandler()
        {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
            {
                final String methodName = method.getName();

                if ("annotationType".equals(methodName))
                {
                    return targetInterface;
                }
                if ("toString".equals(methodName))
                {
                    return "@" + targetInterface.getName() + "(proxied from " + source.toString() + ")";
                }
                if ("hashCode".equals(methodName))
                {
                    return Objects.hash(targetInterface, source);
                }
                if ("equals".equals(methodName))
                {
                    if (args != null && args.length == 1 && args[0] != null)
                    {
                        return args[0] == proxy || source.equals(args[0]);
                    }
                    return false;
                }

                try
                {
                    final Method sourceMethod = source.getClass().getMethod(methodName, method.getParameterTypes());
                    final Object val = sourceMethod.invoke(source, args);

                    if (val != null && val.getClass().isArray() && Annotation.class.isAssignableFrom(val.getClass().getComponentType()))
                    {
                        final Class<?> targetCompType = method.getReturnType().getComponentType();
                        final int len = Array.getLength(val);
                        final Object targetArray = Array.newInstance(targetCompType, len);
                        for (int i = 0; i < len; i++)
                        {
                            final Annotation elementAnno = (Annotation) Array.get(val, i);
                            Array.set(targetArray, i, createAnnotationProxy(elementAnno, (Class<Annotation>) targetCompType));
                        }
                        return targetArray;
                    }

                    if (val instanceof Annotation && Annotation.class.isAssignableFrom(method.getReturnType()))
                    {
                        return createAnnotationProxy((Annotation) val, (Class<Annotation>) method.getReturnType());
                    }

                    return val;
                }
                catch (final NoSuchMethodException e)
                {
                    return method.getDefaultValue();
                }
            }
        };

        return (T) Proxy.newProxyInstance(targetInterface.getClassLoader(), new Class<?>[]
        {
            targetInterface
        }, handler);
    }
}
