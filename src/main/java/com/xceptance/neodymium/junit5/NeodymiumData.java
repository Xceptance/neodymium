package com.xceptance.neodymium.junit5;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.common.browser.BrowserData;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.retry.RetryData;
import com.xceptance.neodymium.common.retry.RetryMethodData;
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
        List<RetryMethodData> retryMethodData = new ArrayList<RetryMethodData>();

        if (workInProgress && wipMethod && templateMethod.getAnnotation(WorkInProgress.class) == null)
        {
            browsers.add(null);
            dataSets.add(null);
            retryMethodData.add(null);
        }
        else
        {
            browsers = browserData.createIterationData(templateMethod);
            dataSets = testdataData.getTestDataForMethod(templateMethod);
            retryMethodData = new RetryData(templateMethod).createIterationData();

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
                for (int i = 0; i < retryMethodData.size(); i++)
                {
                    TemplateInvocationContext context = new TemplateInvocationContext(templateMethod.getName(), browser, dataSet, retryMethodData.get(i), testClass);
                    if (retryMethodData.get(i) != null)
                    {
                        retryMethodData.get(i).setId(testClass.getCanonicalName() + " :: " + context.getDisplayName(i));
                    }
                    multiplicationResult.add(context);
                }
            }
        }
        return multiplicationResult.stream();
    }

}
