package com.xceptance.neodymium.junit4.statement.browser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import com.xceptance.neodymium.common.browser.BrowserBeforeRunner;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.BrowserRunner;
import com.xceptance.neodymium.junit4.EnhancedMethod;

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
