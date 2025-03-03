package com.xceptance.neodymium.junit5.browser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestWatcher;

import com.xceptance.neodymium.common.browser.BrowserAfterRunner;
import com.xceptance.neodymium.common.browser.BrowserBeforeRunner;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.BrowserRunner;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserExecutionCallback implements InvocationInterceptor, BeforeEachCallback, TestWatcher
{
    private BrowserRunner browserRunner;

    private BrowserMethodData browserTag;

    private boolean separateBrowserForSetupRequired;

    private List<Method> afterMethodsWithTestBrowser;

    private boolean setupDone;

    private boolean tearDownDone;

    private boolean testFailed;

    public BrowserExecutionCallback(BrowserMethodData browserTag, String testName)
    {
        this.browserTag = browserTag;
        browserRunner = new BrowserRunner(browserTag, testName);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception
    {
        separateBrowserForSetupRequired = Neodymium.configuration().startNewBrowserForSetUp()
                                          && (browserTag != null ? browserTag.isStartBrowserOnSetUp() : true);
        afterMethodsWithTestBrowser = browserTag == null ? new ArrayList<Method>()
                                                         : new ArrayList<Method>(browserTag.getAfterMethodsWithTestBrowser());
        setupDone = false;
        tearDownDone = false;
        testFailed = false;
        if (browserTag != null)
        {
            if (!separateBrowserForSetupRequired)
            {
                browserRunner.setUpTest();
                setupDone = true;
            }
            else
            {
                Neodymium.setBrowserProfileName(browserTag.getBrowserTag());
            }
        }
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        if (separateBrowserForSetupRequired)
        {
            boolean startForThisBefore = BrowserBeforeRunner.shouldStartNewBrowser(invocationContext.getExecutable());
            if (!startForThisBefore && browserTag != null)
            {
                browserRunner.setUpTest();
                setupDone = true;
                invocation.proceed();
            }
            else if (startForThisBefore)
            {
                new BrowserBeforeRunner().run(() -> {
                    try
                    {
                        invocation.proceed();
                    }
                    catch (Throwable e)
                    {
                        return e;
                    }
                    return null;

                }, invocationContext.getExecutable(), true);
            }
            else
            {
                invocation.proceed();
            }
        }
        else
        {
            invocation.proceed();
        }
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        if (!setupDone && browserTag != null)
        {
            browserRunner.setUpTest();
        }
        try
        {
            invocation.proceed();
        }
        catch (Throwable e)
        {
            testFailed = true;
            throw e;
        }
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        if (Neodymium.configuration().startNewBrowserForCleanUp())
        {
            boolean reuseTestBrowserForThisAfter = afterMethodsWithTestBrowser.remove(invocationContext.getExecutable());
            if (!tearDownDone && !reuseTestBrowserForThisAfter && afterMethodsWithTestBrowser.isEmpty() && browserTag != null)
            {
                browserRunner.teardown(testFailed);
                tearDownDone = true;
                if (browserTag != null)
                {
                    Neodymium.setBrowserProfileName(browserTag.getBrowserTag());
                }
            }
            new BrowserAfterRunner().run(() -> {
                try
                {
                    invocation.proceed();
                }
                catch (Throwable e)
                {
                    return e;
                }
                return null;

            }, invocationContext.getExecutable(), true);
        }
        else
        {
            invocation.proceed();
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause)
    {
        if (!tearDownDone)
        {
            browserRunner.teardown(true);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context)
    {
        if (!tearDownDone)
        {
            browserRunner.teardown(false);
        }
    }
}
