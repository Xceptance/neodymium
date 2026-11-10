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
