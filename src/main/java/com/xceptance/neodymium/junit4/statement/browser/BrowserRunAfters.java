package com.xceptance.neodymium.junit4.statement.browser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.internal.runners.statements.RunAfters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.xceptance.neodymium.common.ScreenshotWriter;
import com.xceptance.neodymium.common.browser.BrowserAfterRunner;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.BrowserRunner;
import com.xceptance.neodymium.junit4.EnhancedMethod;
import com.xceptance.neodymium.util.Neodymium;

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
        BrowserMethodData browserMethodData = method instanceof EnhancedMethod ? (BrowserMethodData) ((EnhancedMethod) method).getData().get(0) : null;
        List<Method> aftersWithTestMethod = Neodymium.configuration().startNewBrowserForCleanUp()
                                                                                                  ? browserMethodData == null ? new ArrayList<Method>()
                                                                                                                              : browserMethodData.getAfterMethodsWithTestBrowser()
                                                                                                  : afters.stream().map(after -> after.getMethod())
                                                                                                          .collect(Collectors.toList());
        boolean tearDownDone = false;
        List<Throwable> errors = new ArrayList<Throwable>();
        try
        {
            next.evaluate();
            ScreenshotWriter.doScreenshot(displayName, className, Optional.empty(), annotationList);
        }
        catch (Throwable e)
        {
            errors.add(e);
            ScreenshotWriter.doScreenshot(displayName, className, Optional.of(e), annotationList);
        }
        finally
        {
            for (FrameworkMethod each : afters)
            {
                if (Neodymium.configuration().startNewBrowserForCleanUp())
                {
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
        }
        MultipleFailureException.assertEmpty(errors);
    }
}
