/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.junit5;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.neodymium.common.WorkInProgress;
import org.neodymium.common.browser.BrowserData;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.retry.RetryData;
import org.neodymium.common.retry.RetryMethodData;
import org.neodymium.common.testdata.TestdataContainer;
import org.neodymium.common.testdata.TestdataData;
import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumAnnotationUtils;

public class NeodymiumData {
    private BrowserData browserData;

    private TestdataData testdataData;

    private Class<?> testClass;

    public NeodymiumData(Class<?> testClass) {
        this.testClass = testClass;
        this.browserData = new BrowserData(testClass);
        this.testdataData = new TestdataData(testClass);
    }

    public Stream<TestTemplateInvocationContext> computeTestMethods(Method templateMethod) {
        boolean workInProgress = Neodymium.configuration().workInProgress();
        boolean wipMethod = List.of(templateMethod.getDeclaringClass().getMethods()).stream()
                .filter(method -> {
                    return (NeodymiumAnnotationUtils.isAnnotationPresent(method, NeodymiumTest.class)
                            || NeodymiumAnnotationUtils.isAnnotationPresent(method, NeodymiumTestGenerator.class));
                })
                .anyMatch(method -> NeodymiumAnnotationUtils.isAnnotationPresent(method, WorkInProgress.class));

        List<TestTemplateInvocationContext> multiplicationResult = new ArrayList<>();
        List<BrowserMethodData> browsers = new ArrayList<BrowserMethodData>();
        List<TestdataContainer> dataSets = new ArrayList<TestdataContainer>();
        List<RetryMethodData> retryMethodData = new ArrayList<RetryMethodData>();

        if (workInProgress && wipMethod && !NeodymiumAnnotationUtils.isAnnotationPresent(templateMethod, WorkInProgress.class)) {
            browsers.add(null);
            dataSets.add(null);
            retryMethodData.add(null);
        } else {
            browsers = browserData.createIterationData(templateMethod);
            dataSets = testdataData.getTestDataForMethod(templateMethod);
            retryMethodData = new RetryData(templateMethod).createIterationData();

            if (browsers.isEmpty()) {
                browsers.add(null);
            }
            if (dataSets.isEmpty()) {
                if (org.apache.commons.lang3.StringUtils.isBlank(Neodymium.configuration().getTestFileFilter())
                        && org.apache.commons.lang3.StringUtils.isBlank(Neodymium.configuration().getTestIdFilter())) {
                    dataSets.add(null);
                }
            }
        }
        for (BrowserMethodData browser : browsers) {
            for (TestdataContainer dataSet : dataSets) {
                for (int i = 0; i < retryMethodData.size(); i++) {
                    TemplateInvocationContext context = new TemplateInvocationContext(templateMethod.getName(), browser,
                            dataSet, retryMethodData.get(i), testClass);
                    if (retryMethodData.get(i) != null) {
                        retryMethodData.get(i).setId(testClass.getCanonicalName() + " :: " + context.getDisplayName(i));
                    }
                    multiplicationResult.add(context);
                }
            }
        }
        return multiplicationResult.stream();
    }

}
