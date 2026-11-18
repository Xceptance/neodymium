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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.neodymium.common.browser.BrowserBeforeRunner;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.browser.BrowserRunner;
import org.neodymium.junit4.EnhancedMethod;

public class BrowserRunBefores extends RunBefores
{
    private FrameworkMethod method;

    private final Statement next;

    private final List<FrameworkMethod> befores;

    private boolean setupDone;

    public BrowserRunBefores(FrameworkMethod method, Statement next, List<FrameworkMethod> befores, Object target)
    {
        super(next, befores, target);
        this.method = method;
        this.befores = befores;
        this.next = next;
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
        boolean startNewBrowserForSetup = browserMethodDataOptional.isPresent() ? ((BrowserMethodData) browserMethodDataOptional.get()).isStartBrowserOnSetUp()
                                                                                : true;
        setupDone = !startNewBrowserForSetup;
        for (FrameworkMethod before : befores)
        {
            if (startNewBrowserForSetup)
            {
                boolean startForThisBefore = BrowserBeforeRunner.shouldStartNewBrowser(before.getMethod());
                boolean isSuppressed = BrowserBeforeRunner.isSuppressed(before.getMethod());
                if (!startForThisBefore && !isSuppressed)
                {
                    BrowserMethodData browserMethodData = (BrowserMethodData) browserMethodDataOptional.get();
                    new BrowserRunner().setUpTest(browserMethodData, method.getDeclaringClass().toString());
                    setupDone = true;
                    invokeMethod(before);
                }
                else if (isSuppressed)
                {
                    invokeMethod(before);
                }
                else if (startForThisBefore)
                {
                    new BrowserBeforeRunner().run(() -> {
                        try
                        {
                            invokeMethod(before);
                        }
                        catch (Throwable e)
                        {
                            return e;
                        }
                        return null;
                    }, before.getMethod(), false);
                }
            }
            else
            {
                invokeMethod(before);
            }
        }
        if (!setupDone && browserMethodDataOptional.isPresent())
        {
            new BrowserRunner().setUpTest((BrowserMethodData) browserMethodDataOptional.get(), method.getDeclaringClass().toString());
        }
        next.evaluate();

    }
}
