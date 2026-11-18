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
package org.neodymium.junit4.statement.browser;

import org.neodymium.common.browser.BrowserAfterRunner;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.browser.BrowserRunner;
import org.neodymium.junit4.EnhancedMethod;
import org.neodymium.util.Neodymium;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BrowserRunAfters extends RunAfters
{
    private final FrameworkMethod method;

    private final Statement next;

    private final List<FrameworkMethod> afters;

    private final String displayName;

    private final String className;

    private final Annotation[] annotationList;

    public BrowserRunAfters(FrameworkMethod method, Statement next, List<FrameworkMethod> afters, Object target)
    {
        super(next, afters, target);
        this.method = method;
        this.afters = afters;
        this.next = next;

        this.displayName = method.getMethod().getName().replaceAll("\\d+\\s\\/\\s\\d+", "");
        this.className = method.getDeclaringClass()
                               .getCanonicalName();
        this.annotationList = method.getAnnotations();
    }

    @Override
    public void evaluate() throws Throwable
    {
        Optional<Object> browserMethodDataOptional = method instanceof EnhancedMethod ? ((EnhancedMethod) method).getData()
                                                                                                                 .stream()
                                                                                                                 .filter(data -> data instanceof BrowserMethodData)
                                                                                                                 .findFirst()
                                                                                      : Optional.empty();
        List<Method> afterMethodsToBeExecutedWithTestBrowser = new ArrayList<Method>();
        List<Method> aftersWithTestMethod = new ArrayList<Method>();
        if (browserMethodDataOptional.isPresent())
        {
            BrowserMethodData browserMethodData = (BrowserMethodData) browserMethodDataOptional.get();
            afterMethodsToBeExecutedWithTestBrowser = new ArrayList<Method>(browserMethodData.getAfterMethodsWithTestBrowser());
            aftersWithTestMethod = Neodymium.configuration().startNewBrowserForCleanUp()
                                                                                         ? browserMethodData == null ? new ArrayList<Method>()
                                                                                                                     : afterMethodsToBeExecutedWithTestBrowser
                                                                                         : afters.stream().map(after -> after.getMethod())
                                                                                                 .collect(Collectors.toList());
        }
        boolean tearDownDone = false;
        List<Throwable> errors = new ArrayList<Throwable>();
        try
        {
            next.evaluate();
        }
        catch (Throwable e)
        {
            errors.add(e);
        }
        for (FrameworkMethod each : afters)
        {
            if (Neodymium.configuration().startNewBrowserForCleanUp())
            {
                if (browserMethodDataOptional.isPresent())
                {
                    BrowserMethodData browserMethodData = (BrowserMethodData) browserMethodDataOptional.get();
                    boolean reuseTestBrowserForThisAfter = aftersWithTestMethod.remove(each.getMethod());
                    if (!tearDownDone && !reuseTestBrowserForThisAfter && aftersWithTestMethod.isEmpty() && browserMethodData != null)
                    {
                        new BrowserRunner().teardown(!errors.isEmpty(), browserMethodData, Neodymium.getWebDriverStateContainer());
                        tearDownDone = true;
                        if (browserMethodData != null)
                        {
                            Neodymium.setBrowserProfileName(browserMethodData.getBrowserTag());
                        }
                    }
                }

                new BrowserAfterRunner().run(() -> {
                    try
                    {
                        invokeMethod(each);
                    }
                    catch (Throwable e)
                    {
                        return e;
                    }
                    return null;
                }, each.getMethod(), false);
            }
            else
            {
                invokeMethod(each);
            }
        }

        MultipleFailureException.assertEmpty(errors);
    }
}
