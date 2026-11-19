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
