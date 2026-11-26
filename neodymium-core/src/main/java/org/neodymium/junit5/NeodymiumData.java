/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
