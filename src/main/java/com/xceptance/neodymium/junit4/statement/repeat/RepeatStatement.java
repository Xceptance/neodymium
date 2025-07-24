package com.xceptance.neodymium.junit4.statement.repeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assume;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.junit4.StatementBuilder;

public class RepeatStatement extends StatementBuilder<Object>
{
    private static final Map<Thread, List<Boolean>> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private Statement next;

    private int iterationIndex;

    private int maxRepetitionNumber;

    public RepeatStatement(Statement next, RepeatOnFailureData data)
    {
        this.next = next;
        this.iterationIndex = data.getIterationNumber();
        this.maxRepetitionNumber = data.getMaxNumber();
    }

    public RepeatStatement()
    {
    }

    private static List<Boolean> getContext()
    {
        return CONTEXTS.computeIfAbsent(Thread.currentThread(), key -> {
            return new ArrayList<>();
        });
    }

    @Override
    public List<Object> createIterationData(TestClass testClass, FrameworkMethod method) throws Throwable
    {
        RepeatOnFailure classRepeatOnFailure = testClass.getAnnotation(RepeatOnFailure.class);
        List<RepeatOnFailure> methodRepeatOnFailures = Data.getAnnotations(method.getMethod(), RepeatOnFailure.class);
        int numberOfRepetitions = 0;
        if (classRepeatOnFailure != null)
        {
            numberOfRepetitions = classRepeatOnFailure.value();

        }
        if (!methodRepeatOnFailures.isEmpty())
        {
            numberOfRepetitions = methodRepeatOnFailures.get(0).value();
        }
        final int maxRepetitionNumber = numberOfRepetitions;
        return IntStream.rangeClosed(1, numberOfRepetitions)
                        .boxed().map(x -> new RepeatOnFailureData(x, maxRepetitionNumber)).collect(Collectors.toList());
    }

    @Override
    public StatementBuilder createStatement(Object testClassInstance, Statement next, Object parameter)
    {
        return new RepeatStatement(next, (RepeatOnFailureData) parameter);
    }

    @Override
    public String getTestName(Object data)
    {
        RepeatOnFailureData d = (RepeatOnFailureData) data;
        return "Run number " + d.getIterationNumber();
    }

    @Override
    public String getCategoryName(Object data)
    {
        return "Repeat on failure test";
    }

    @Override
    public void evaluate() throws Throwable
    {
        getContext().add(true);
        boolean toExecute = getContext().size() < 2 ||
                            !getContext().get(iterationIndex - 2);
        if (iterationIndex == maxRepetitionNumber)
        {
            CONTEXTS.remove(Thread.currentThread());
        }
        Assume.assumeTrue("Original test succeeded", toExecute);
        try
        {
            next.evaluate();
        }
        catch (Throwable t)
        {
            if (iterationIndex != maxRepetitionNumber)
            {
                getContext().set(iterationIndex - 1, false);
            }
            throw t;
        }
    }
}
