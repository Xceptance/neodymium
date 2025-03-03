package com.xceptance.neodymium.common.browser;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserData extends Data
{
    private List<String> classBrowsers;

    private List<String> systemBrowserFilter;

    private List<BrowserMethodData> browserMethodDatas = new LinkedList<>();

    private List<RandomBrowsers> classRandomBrowsersAnnotation;

    private Class<?> testClass;

    private static final String SYSTEM_PROPERTY_BROWSERDEFINITION = "browserdefinition";

    public BrowserData(Class<?> testClass)
    {
        this();
        initClassAnnotationsFor(testClass);
    }

    public void initClassAnnotationsFor(Class<?> testClass)
    {
        this.testClass = testClass;
        classRandomBrowsersAnnotation = getAnnotations(testClass, RandomBrowsers.class);

        if (getAnnotations(testClass, SuppressBrowsers.class).isEmpty())
        {
            classBrowsers = findBrowserRelatedClassAnnotation(testClass, Browser.class).stream().map(annotation -> annotation.value()).distinct()
                                                                                       .collect(Collectors.toList());
        }
        else
        {
            classBrowsers = new LinkedList<>();
        }
    }

    public <T extends Annotation> List<T> findBrowserRelatedClassAnnotation(final Class<?> clazz, final Class<T> annotationToFind)
    {
        // this function is used to find the first (!) @Browser annotation on class level in the hierarchy
        // furthermore its not the first but also the first that doesn't have @SuppressBrowsers annotated

        if (clazz == null)
            return new LinkedList<>();

        // check class for browser annotation
        // if class has browser annotation and no suppress browsers its fine, else take the super class and check again
        List<T> browserAnnotations = getDeclaredAnnotations(clazz, annotationToFind);
        List<SuppressBrowsers> suppressBrowsersAnnotations = getDeclaredAnnotations(clazz, SuppressBrowsers.class);

        if (!suppressBrowsersAnnotations.isEmpty() || browserAnnotations.isEmpty())
        {
            return findBrowserRelatedClassAnnotation(clazz.getSuperclass(), annotationToFind);
        }
        else
        {
            return browserAnnotations;
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

    public BrowserData()
    {
        populateBrowserDataWithGlobalInformation();
    }

    private void populateBrowserDataWithGlobalInformation()
    {
        final String ieDriverPath = Neodymium.configuration().getIeDriverPath();
        final String edgeDriverPath = Neodymium.configuration().getEdgeDriverPath();
        final String chromeDriverPath = Neodymium.configuration().getChromeDriverPath();
        final String geckoDriverPath = Neodymium.configuration().getFirefoxDriverPath();

        if (!StringUtils.isEmpty(ieDriverPath))
        {
            System.setProperty("webdriver.ie.driver", ieDriverPath);
        }
        if (!StringUtils.isEmpty(chromeDriverPath))
        {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        }
        if (!StringUtils.isEmpty(geckoDriverPath))
        {
            System.setProperty("webdriver.gecko.driver", geckoDriverPath);
        }
        if (!StringUtils.isEmpty(edgeDriverPath))
        {
            System.setProperty("webdriver.edge.driver", edgeDriverPath);
        }

        // TODO: do we need a possibility to define browser tags globaly via system var? Is this opportunity documented?

        // get test specific browser definitions (aka browser tag see browser.properties)
        // could be one value or comma separated list of values
        String browserDefinitionsProperty = System.getProperty(SYSTEM_PROPERTY_BROWSERDEFINITION, "");
        browserDefinitionsProperty = browserDefinitionsProperty.replaceAll("\\s", "");

        // parse test specific browser definitions
        if (!StringUtils.isEmpty(browserDefinitionsProperty))
        {
            systemBrowserFilter.addAll(Arrays.asList(browserDefinitionsProperty.split(",")));
        }
    }

    public List<BrowserMethodData> getBrowserTags()
    {
        return browserMethodDatas;
    }

    public List<BrowserMethodData> createIterationData(Method testMethod)
    {
        List<String> browsers = new LinkedList<>();
        List<String> methodBrowsers = new LinkedList<>();
        if (getAnnotations(testMethod, SuppressBrowsers.class).isEmpty())
        {
            methodBrowsers = getAnnotations(testMethod, Browser.class).stream().map(annotation -> annotation.value()).distinct()
                                                                      .collect(Collectors.toList());
            if (!methodBrowsers.isEmpty())
            {
                browsers = methodBrowsers;
            }
            else
            {
                browsers = classBrowsers;
            }
            List<RandomBrowsers> methodRandomBrowsersAnnotations = getAnnotations(testMethod, RandomBrowsers.class);

            // choose a random set from the available browser annotations
            if (!methodRandomBrowsersAnnotations.isEmpty())
            {
                // evaluate the method level (top priority)
                browsers = computeRandomBrowsers(testMethod, methodRandomBrowsersAnnotations, browsers);
            }
            else if (!classRandomBrowsersAnnotation.isEmpty() && methodBrowsers.isEmpty())
            {
                // evaluate the class level (including inheritance)
                // Note: if browsers are annotated on method level they prohibit the evaluation of the random
                // browser annotation on class level
                browsers = computeRandomBrowsers(testMethod, classRandomBrowsersAnnotation, browsers);
            }
        }
        if (systemBrowserFilter != null && !systemBrowserFilter.isEmpty())
        {
            return browsers.stream()
                           .filter(browserTag -> systemBrowserFilter.contains(browserTag))
                           .map(browserTag -> addKeepBrowserOpenInformation(browserTag, testMethod))
                           .collect(Collectors.toList());
        }
        return browsers.stream()
                       .map(browserTag -> addKeepBrowserOpenInformation(browserTag, testMethod))
                       .collect(Collectors.toList());
    }

    public static BrowserMethodData addKeepBrowserOpenInformationForBeforeOrAfter(String browserTag, Method method)
    {
        List<KeepBrowserOpen> methodKeepBrowserOpenAnnotations = getAnnotations(method, KeepBrowserOpen.class);
        List<KeepBrowserOpen> classKeepBrowserOpenAnnotations = getAnnotations(method.getDeclaringClass(), KeepBrowserOpen.class);

        boolean keepOpen = Neodymium.configuration().keepBrowserOpen();
        boolean keepOpenOnFailure = Neodymium.configuration().keepBrowserOpenOnFailure();

        if (!classKeepBrowserOpenAnnotations.isEmpty())
        {
            KeepBrowserOpen keepBrowserOpen = classKeepBrowserOpenAnnotations.get(0);
            if (keepBrowserOpen.onlyOnFailure())
            {
                keepOpen = false;
                keepOpenOnFailure = true;
            }
            else
            {
                keepOpen = true;
                keepOpenOnFailure = false;
            }
        }

        if (!methodKeepBrowserOpenAnnotations.isEmpty())
        {
            KeepBrowserOpen keepBrowserOpen = methodKeepBrowserOpenAnnotations.get(0);
            if (keepBrowserOpen.onlyOnFailure())
            {
                keepOpen = false;
                keepOpenOnFailure = true;
            }
            else
            {
                keepOpen = true;
                keepOpenOnFailure = false;
            }
        }

        return new BrowserMethodData(browserTag, keepOpen, keepOpenOnFailure, false, false, new ArrayList<Method>());
    }

    private BrowserMethodData addKeepBrowserOpenInformation(String browserTag, Method method)
    {
        BrowserMethodData browserMethodData = addKeepBrowserOpenInformationForBeforeOrAfter(browserTag, method);
        boolean junit5 = method.getAnnotation(NeodymiumTest.class) != null;
        List<Method> afterMethodsWithTestBrowser = List.of(testClass.getMethods()).stream()
                                                       .filter(classMethod -> (junit5 ? classMethod.getAnnotation(AfterEach.class)
                                                                                      : classMethod.getAnnotation(After.class)) != null)
                                                       .collect(Collectors.toList());
        if (!(Neodymium.configuration().startNewBrowserForSetUp() && Neodymium.configuration().startNewBrowserForCleanUp()))
        {
            browserMethodData.setAfterMethodsWithTestBrowser(afterMethodsWithTestBrowser);
            return browserMethodData;

        }
        boolean separateBrowserForSetupRequired = false;
        boolean separateBrowserForCleanupRequired = false;

        if (Neodymium.configuration().startNewBrowserForSetUp())
        {
            separateBrowserForSetupRequired = Neodymium.configuration().startNewBrowserForSetUp()
                                              && (testClass.getAnnotation(StartNewBrowserForSetUp.class) != null
                                                  || List.of(testClass.getMethods()).stream()
                                                         .filter(classMethod -> (junit5 ? classMethod.getAnnotation(BeforeEach.class)
                                                                                        : classMethod.getAnnotation(Before.class)) != null
                                                                                && classMethod.getAnnotation(StartNewBrowserForSetUp.class) != null)
                                                         .findAny().isPresent());
        }
        if (Neodymium.configuration().startNewBrowserForCleanUp())
        {
            List<Method> afterMethods = new ArrayList<Method>(afterMethodsWithTestBrowser);
            afterMethodsWithTestBrowser = new ArrayList<Method>();
            if (testClass.getAnnotation(StartNewBrowserForCleanUp.class) == null)
            {
                afterMethodsWithTestBrowser = afterMethods.stream().filter(classMethod -> classMethod.getAnnotation(StartNewBrowserForCleanUp.class) == null)
                                                          .collect(Collectors.toList());
            }
            else
            {
                afterMethodsWithTestBrowser = afterMethods.stream().filter(classMethod -> classMethod.getAnnotation(SuppressBrowsers.class) != null)
                                                          .collect(Collectors.toList());
            }
            separateBrowserForCleanupRequired = afterMethodsWithTestBrowser.isEmpty() && !afterMethods.isEmpty();
        }

        browserMethodData.setStartBrowserOnSetUp(separateBrowserForSetupRequired);
        browserMethodData.setStartBrowserOnCleanUp(separateBrowserForCleanupRequired);
        browserMethodData.setAfterMethodsWithTestBrowser(afterMethodsWithTestBrowser);
        return browserMethodData;
    }

    private List<String> computeRandomBrowsers(final Method method, final List<RandomBrowsers> randomBrowsersAnnotation,
                                               final List<String> browsers)
    {
        if (randomBrowsersAnnotation.get(0).value() > browsers.size())
        {
            String msg = MessageFormat.format("Method ''{0}'' is marked to be run with {1} random browsers, but there are only {2} available",
                                              method.getName(), randomBrowsersAnnotation.get(0).value(), browsers.size());
            throw new IllegalArgumentException(msg);
        }
        if (randomBrowsersAnnotation.get(0).value() > 0)
        {
            Collections.shuffle(browsers, Neodymium.getRandom());
            return browsers.subList(0, randomBrowsersAnnotation.get(0).value());
        }
        return browsers;
    }
}
