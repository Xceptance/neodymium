package com.xceptance.neodymium.junit4.statement.browser;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.internal.runners.statements.RunAfters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.xceptance.neodymium.common.ScreenshotWriter;

public class ScreenshotRunAfters extends RunAfters
{
    private final String displayName;

    private final String className;

    private final Statement next;

    private final List<FrameworkMethod> afters;

    private final Annotation[] annotationList;

    public ScreenshotRunAfters(FrameworkMethod method, Statement next, List<FrameworkMethod> afters, Object target)
    {
        super(next, afters, target);
        this.displayName = method.getMethod().getName().replaceAll("\\d+\\s\\/\\s\\d+", "");
        this.className = method.getDeclaringClass()
                               .getCanonicalName();
        this.annotationList = method.getAnnotations();
        this.afters = afters;
        this.next = next;
    }

    @Override
    public void evaluate() throws Throwable
    {
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
                try
                {
                    invokeMethod(each);
                }
                catch (Throwable e)
                {
                    errors.add(e);
                }
            }
        }
        MultipleFailureException.assertEmpty(errors);
    }
}
