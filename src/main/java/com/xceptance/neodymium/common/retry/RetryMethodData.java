package com.xceptance.neodymium.common.retry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang3.StringUtils;

import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;

public class RetryMethodData
{
    private static final Map<String, AllureData> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<String, Throwable> ERROR_CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private List<String> exceptions;

    private int maxExecutions;

    private int iterationIndex;

    private String id;

    public RetryMethodData(List<String> exceptions, int maxExecutions, int iterationIndex)
    {
        this.exceptions = exceptions == null ? new ArrayList<String>() : exceptions;
        // this.exceptions.add("Abort test and retry for the");
        this.maxExecutions = maxExecutions;
        this.iterationIndex = iterationIndex;
    }
    

    public RetryMethodData(RetryMethodData retryMethodData)
    {
        this.exceptions = retryMethodData.exceptions;
        // this.exceptions.add("Abort test and retry for the");
        this.maxExecutions = retryMethodData.maxExecutions;
        this.iterationIndex = retryMethodData.iterationIndex;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    private String getId()
    {
        return StringUtils.isBlank(id) ? Thread.currentThread().getId() + "" : id;
    }

    public AllureData getContext()
    {
        String testSingature = Neodymium.configuration().setProperty("testSignature", "temp");
        Neodymium.configuration().setProperty("testSignature", testSingature);
        return CONTEXTS.computeIfAbsent(getId(), key -> {
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
        String testSingature = Neodymium.configuration().setProperty("testSignature", "temp");
        Neodymium.configuration().setProperty("testSignature", testSingature);
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
        boolean shouldNotBeRepeated = (ERROR_CONTEXTS.get(getId()) == null
                                       || !exceptions.isEmpty() && !exceptions.stream()
                                                                              .anyMatch(expExc -> ERROR_CONTEXTS.get(getId())
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
            CONTEXTS.remove(getId());
            ERROR_CONTEXTS.remove(getId());
        }
        // if (!shouldNotBeRepeated)
        // {
        // Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        // }
        return iterationIndex > 0 && shouldNotBeRepeated;
    }

    public Throwable handleTestExecutionException(Throwable throwable) throws Throwable
    {
        String testSingature = Neodymium.configuration().setProperty("testSignature", "temp");
        Neodymium.configuration().setProperty("testSignature", testSingature);
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
            CONTEXTS.remove(getId());
            ERROR_CONTEXTS.remove(getId());
            // Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        }
        else
        {
            ERROR_CONTEXTS.put(getId(), throwable);
        }
        boolean shouldNotBeRepeated = (ERROR_CONTEXTS.get(getId()) == null
                                       || !exceptions.isEmpty() && !exceptions.stream()
                                                                              .anyMatch(expExc -> ERROR_CONTEXTS.get(getId())
                                                                                                                .getMessage()
                                                                                                                .contains(expExc)));
        if (maxExecutions > 1 && (!shouldNotBeRepeated || iterationIndex == maxExecutions - 1))
        {
            Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        }
        return getThrowable(throwable);
    }

    public Throwable getThrowable(Throwable throwable)
    {
        if (iterationIndex < maxExecutions - 1 && (throwable != null && (exceptions.isEmpty()
                                                                         || exceptions.stream()
                                                                                      .anyMatch(expExc -> throwable.getMessage()
                                                                                                                   .contains(expExc)))))
        {
            Allure.getLifecycle().updateTestCase(r -> r.setStage(Stage.FINISHED));
            Allure.getLifecycle().updateTestCase(r -> r.setStatus(Status.FAILED));
            Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setMuted(false).setMessage(throwable.getMessage())));
            return new TestFailedAndShouldBeRetired(iterationIndex + 1, throwable);
        }
        else
        {
            return throwable;
        }
    }

    public void testFailed(Throwable cause)
    {
        String testSingature = Neodymium.configuration().setProperty("testSignature", "temp");
        Neodymium.configuration().setProperty("testSignature", testSingature);
        if (iterationIndex != maxExecutions - 1)
        {
            ERROR_CONTEXTS.put(getId(), cause);
        }
    }

    public void testSuccessful()
    {
        String testSingature = Neodymium.configuration().setProperty("testSignature", "temp");
        Neodymium.configuration().setProperty("testSignature", testSingature);
        ERROR_CONTEXTS.remove(getId());
    }
}
