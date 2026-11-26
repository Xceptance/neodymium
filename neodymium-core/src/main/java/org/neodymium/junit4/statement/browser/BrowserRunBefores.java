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
