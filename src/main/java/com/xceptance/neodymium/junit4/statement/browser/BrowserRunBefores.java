package com.xceptance.neodymium.junit4.statement.browser;

import java.util.List;

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
        BrowserMethodData browserMethodData = method instanceof EnhancedMethod ? (BrowserMethodData) ((EnhancedMethod) method).getData().get(0) : null;
        boolean startNewBrowserForSetup = browserMethodData != null ? browserMethodData.isStartBrowserOnSetUp() : true;
        setupDone = !startNewBrowserForSetup;
        for (FrameworkMethod before : befores)
        {
            if (startNewBrowserForSetup)
            {
                boolean startForThisBefore = BrowserBeforeRunner.shouldStartNewBrowser(before.getMethod());
                boolean isSuppressed = BrowserBeforeRunner.isSuppressed(before.getMethod());
                if (!startForThisBefore && !isSuppressed)
                {
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
        if (!setupDone && browserMethodData != null)
        {
            new BrowserRunner().setUpTest(browserMethodData, method.getDeclaringClass().toString());
        }
        next.evaluate();

    }
}
