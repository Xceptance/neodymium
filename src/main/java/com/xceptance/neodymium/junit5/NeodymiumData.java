package com.xceptance.neodymium.junit5;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.common.browser.BrowserData;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.TestdataContainer;
import com.xceptance.neodymium.common.testdata.TestdataData;
import com.xceptance.neodymium.util.Neodymium;

public class NeodymiumData
{
    private BrowserData browserData;

    private TestdataData testdataData;

    private Class<?> testClass;

    public NeodymiumData(Class<?> testClass)
    {
        this.testClass = testClass;
        this.browserData = new BrowserData(testClass);
        this.testdataData = new TestdataData(testClass);
    }

    public Stream<TestTemplateInvocationContext> computeTestMethods(Method templateMethod)
    {
        boolean workInProgress = Neodymium.configuration().workInProgress();
        boolean wipMethod = List.of(templateMethod.getDeclaringClass().getMethods()).stream()
                                .filter(method -> method.getAnnotation(NeodymiumTest.class) != null)
                                .anyMatch(method -> method.getAnnotation(WorkInProgress.class) != null);

        List<TestTemplateInvocationContext> multiplicationResult = new ArrayList<>();
        List<BrowserMethodData> browsers = new ArrayList<BrowserMethodData>();
        List<TestdataContainer> dataSets = new ArrayList<TestdataContainer>();

        if (workInProgress && wipMethod && templateMethod.getAnnotation(WorkInProgress.class) == null)
        {
            browsers.add(null);
            dataSets.add(null);
        }
        else
        {
            browsers = browserData.createIterationData(templateMethod);
            dataSets = testdataData.getTestDataForMethod(templateMethod);

            boolean classHasSuppressBrowsers = Arrays.stream(templateMethod.getDeclaringClass().getAnnotations())
                                                     .anyMatch(annotation -> annotation.annotationType().equals(SuppressBrowsers.class));
            boolean methodHasSuppressBrowsers = Arrays.stream(templateMethod.getAnnotations())
                                                      .anyMatch(annotation -> annotation.annotationType().equals(SuppressBrowsers.class));

            // case no @Browser annotation, so fall back to default browser if the browser is not suppressed
            if (browsers.isEmpty() && !classHasSuppressBrowsers && !methodHasSuppressBrowsers)
            {
                boolean hasNewBrowserForSetUpAnnotation = List.of(templateMethod.getDeclaringClass().getMethods()).stream()
                                                              .anyMatch(method -> method.getAnnotation(StartNewBrowserForSetUp.class) != null);
                boolean hasNewBrowserForCleanUpAnnotation = List.of(templateMethod.getDeclaringClass().getMethods()).stream()
                                                                .anyMatch(method -> method.getAnnotation(StartNewBrowserForCleanUp.class) != null);

                browsers.add(new BrowserMethodData("Chrome_Default", Neodymium.configuration()
                                                                              .keepBrowserOpen(), Neodymium.configuration()
                                                                                                           .keepBrowserOpenOnFailure(), hasNewBrowserForSetUpAnnotation, hasNewBrowserForCleanUpAnnotation, new ArrayList<Method>()));
            }
            if (browsers.isEmpty())
            {
                browsers.add(null);
            }
            if (dataSets.isEmpty())
            {
                dataSets.add(null);
            }
        }
        for (BrowserMethodData browser : browsers)
        {
            for (TestdataContainer dataSet : dataSets)
            {
                multiplicationResult.add(new TemplateInvocationContext(templateMethod.getName(), browser, dataSet, testClass));
            }
        }
        return multiplicationResult.stream();
    }
}
