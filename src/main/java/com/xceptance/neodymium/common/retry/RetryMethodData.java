package com.xceptance.neodymium.common.retry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;

public class RetryMethodData
{
    private static final Map<Thread, AllureData> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<Thread, Throwable> ERROR_CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private List<String> exceptions;

    private int maxExecutions;

    private int iterationIndex;

    public RetryMethodData(List<String> exceptions, int maxExecutions, int iterationIndex)
    {
        this.exceptions = exceptions == null ? new ArrayList<String>() : exceptions;
        // this.exceptions.add("Abort test and retry for the");
        this.maxExecutions = maxExecutions;
        this.iterationIndex = iterationIndex;
    }

    public static AllureData getContext()
    {
        return CONTEXTS.computeIfAbsent(Thread.currentThread(), key -> {
            return new AllureData();
        });
    }

    public List<String> getExceptions()
    {
        return exceptions;
    }

    public int getMaxExecutions()
    {
        return maxExecutions;
    }

    public int getIterationIndex()
    {
        return iterationIndex;
    }

    public boolean shouldNotBeRepeated()
    {
        if (iterationIndex == 0 && Allure.getLifecycle().getCurrentTestCase().isPresent())
        {
            // LogManager.getLogManager().getLogger("io.qameta.allure.AllureLifecycle").setLevel(Level.OFF);
            Allure.getLifecycle().updateTestCase(r -> getContext().setStart(r.getStart()));
            Allure.getLifecycle().updateTestCase(r -> getContext().setLabelId(r.getLabels().stream()
                                                                               .filter(l -> l.getName().equals("junit.platform.uniqueid")).findFirst().get()
                                                                               .getValue()));
            // Allure.getLifecycle().updateTestCase(r -> getContext().setParameterId(r.getParameters().stream()
            // .filter(l -> l.getName().equals("UniqueId")).findFirst().get().getValue()));

            getContext().setUuid(Allure.getLifecycle().getCurrentTestCase().get());
            Allure.getLifecycle()

                  .updateTestCase(r -> getContext().setHistoryId(r.getHistoryId()));
            Allure.getLifecycle().updateTestCase(r -> r.setHistoryId(getContext().getHistoryId()));
        }
        boolean shouldNotBeRepeated = (ERROR_CONTEXTS.get(Thread.currentThread()) == null
                                       || !exceptions.isEmpty() && !exceptions.stream()
                                                                              .anyMatch(expExc -> ERROR_CONTEXTS.get(Thread.currentThread())
                                                                                                                .getMessage()
                                                                                                                .contains(expExc)));
        if (iterationIndex > 0 && Allure.getLifecycle().getCurrentTestCase().isPresent())
        {
            // Configurator.setLevel("io.qameta.allure.AllureLifecycle", Level.INFO);
            // java.util.logging.Logger.getLogger("io.qameta.allure.AllureLifecycle").setLevel(java.util.logging.Level.OFF);
            // LogManager.getLogManager().getLogger("io.qameta.allure.AllureLifecycle").setLevel(Level.OFF);
            Allure.getLifecycle().updateTestCase(r -> r.setTestCaseId(getContext().getLabelId()));
            Allure.getLifecycle().updateTestCase(r -> r.setHistoryId(getContext().getHistoryId()));
            // Allure.getLifecycle()
            // .updateTestCase(r -> r.getParameters().stream().filter(l -> l.getName().equals("UniqueId"))
            // .findFirst().get().setValue(getContext().getParameterId()));
            Allure.getLifecycle().updateTestCase(
                                                 r -> r.getLabels().stream().filter(l -> l.getName().equals("junit.platform.uniqueid")).findFirst()
                                                       .get().setValue(getContext().getLabelId()));
            if (shouldNotBeRepeated)
            {
                Allure.getLifecycle().updateTestCase(r -> r.setStart(getContext().getStart() - 1));
                String uuidTest = Allure.getLifecycle().getCurrentTestCase().get();
                Allure.getLifecycle().stopTestCase(uuidTest);
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStage(Stage.FINISHED));
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStatus(Status.SKIPPED));
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStatusDetails(new StatusDetails().setMuted(true).setMessage("skip repetition")));
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStop(getContext().getStart() - 1));
                Allure.getLifecycle().writeTestCase(uuidTest);
                // java.util.logging.Logger.getLogger("io.qameta.allure.AllureLifecycle").setLevel(java.util.logging.Level.OFF);
                // LogManager.getLogManager().getLogger("io.qameta.allure.AllureLifecycle").setLevel(Level.OFF);

                // Configurator.setLevel("io.qameta.allure.AllureLifecycle", Level.FATAL);
            }
        }
        if (iterationIndex == maxExecutions)
        {
            CONTEXTS.remove(Thread.currentThread());
            ERROR_CONTEXTS.remove(Thread.currentThread());
        }
        // if (!shouldNotBeRepeated)
        // {
        // Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        // }
        return iterationIndex > 0 && shouldNotBeRepeated;
    }

    public void handleTestExecutionException(Throwable throwable) throws Throwable
    {
        // if (iterationIndex < maxExecutions - 1 && (throwable != null && (exceptions.isEmpty()
        // || exceptions.stream()
        // .anyMatch(expExc -> throwable.getMessage()
        // .contains(expExc)))))
        // {
        // }
        if (iterationIndex > 0)
        {
            // LogManager.getLogManager().getLogger("io.qameta.allure.AllureLifecycle").setLevel(Level.OFF);

            Allure.getLifecycle().updateTestCase(r -> r.setTestCaseId(getContext().getLabelId()));
            Allure.getLifecycle().updateTestCase(r -> r.setHistoryId(getContext().getHistoryId()));
            // Allure.getLifecycle()
            // .updateTestCase(r -> r.getParameters().stream().filter(l -> l.getName().equals("UniqueId"))
            // .findFirst().get().setValue(getContext().getParameterId()));
            Allure.getLifecycle().updateTestCase(
                                                 r -> r.getLabels().stream().filter(l -> l.getName().equals("junit.platform.uniqueid")).findFirst()
                                                       .get().setValue(getContext().getLabelId()));
            // LogManager.getLogManager().getLogger("io.qameta.allure.AllureLifecycle").setLevel(Level.OFF);
        }

        if (iterationIndex == maxExecutions - 1)
        {
            CONTEXTS.remove(Thread.currentThread());
            ERROR_CONTEXTS.remove(Thread.currentThread());
            // Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        }
        else
        {
            ERROR_CONTEXTS.put(Thread.currentThread(), throwable);
        }
        boolean shouldNotBeRepeated = (ERROR_CONTEXTS.get(Thread.currentThread()) == null
                                       || !exceptions.isEmpty() && !exceptions.stream()
                                                                              .anyMatch(expExc -> ERROR_CONTEXTS.get(Thread.currentThread())
                                                                                                                .getMessage()
                                                                                                                .contains(expExc)));
        if (maxExecutions > 1 && (!shouldNotBeRepeated || iterationIndex == maxExecutions - 1))
        {
            Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        }
    }

    public void testFailed(Throwable cause)
    {
        if (iterationIndex != maxExecutions - 1)
        {
            ERROR_CONTEXTS.put(Thread.currentThread(), cause);
        }
    }

    public void testSuccessful()
    {
        ERROR_CONTEXTS.remove(Thread.currentThread());
    }
}
