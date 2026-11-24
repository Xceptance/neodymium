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
package io.cucumber.junit;

import java.io.IOException;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;

/**
 * Custom runner bridge placed in {@code io.cucumber.junit} to integrate Neodymium's execution
 * lifecycle with Cucumber JUnit 4 protected members.
 *
 * @author Xceptance Software Technologies GmbH 2026
 */
public class NeodymiumCucumberWrapper extends ParentRunner<ParentRunner<?>>
{
    private Cucumber cucumber;

    public NeodymiumCucumberWrapper(Class<?> clazz) throws InitializationError, IOException
    {
        super(clazz);
        cucumber = new Cucumber(clazz);
    }

    // from Cucumber
    @Override
    protected List<ParentRunner<?>> getChildren()
    {
        return cucumber.getChildren();
    }

    @Override
    protected Description describeChild(ParentRunner<?> child)
    {
        return cucumber.describeChild(child);
    }

    @Override
    protected void runChild(ParentRunner<?> child, RunNotifier notifier)
    {
        cucumber.runChild(child, notifier);
    }

    @Override
    protected Statement childrenInvoker(RunNotifier notifier)
    {
        return cucumber.childrenInvoker(notifier);
    }

    @Override
    public void setScheduler(RunnerScheduler scheduler)
    {
        cucumber.setScheduler(scheduler);
    }

    // from ParentsRunner
    @Override
    public void run(RunNotifier notifier)
    {
        cucumber.run(notifier);
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException
    {
        cucumber.filter(filter);
    }
}
