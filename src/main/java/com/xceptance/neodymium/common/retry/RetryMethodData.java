package com.xceptance.neodymium.common.retry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang3.StringUtils;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;

/**
 * This class represents the object that is used to:
 * <ul>
 * <li>hold information about the specific iteration of the single test method and do the manipulations</li>
 * <li>give information if the iteration should be executed and what exception should be thrown as iteration result
 * <li>manipulate Allure properties of the test to make it be displayed in the report properly
 * </ul>
 */
public class RetryMethodData
{
    private static final Map<String, AllureData> ALLURE_CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<String, Throwable> ERROR_CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private List<String> exceptions;

    private int maxExecutions;

    private int iterationIndex;

    private String id;

    /**
     * Constructor to initialize the object and its properties
     * 
     * @param exceptions
     * @param maxExecutions
     * @param iterationIndex
     */
    public RetryMethodData(List<String> exceptions, int maxExecutions, int iterationIndex)
    {
        this.exceptions = exceptions == null ? new ArrayList<String>() : exceptions;
        this.maxExecutions = maxExecutions;
        this.iterationIndex = iterationIndex;
    }

    /**
     * Constructor to copy object
     * 
     * @param retryMethodData
     *            {@link RetryMethodData} object to copy
     */
    public RetryMethodData(RetryMethodData retryMethodData)
    {
        this.exceptions = retryMethodData.exceptions;
        this.maxExecutions = retryMethodData.maxExecutions;
        this.iterationIndex = retryMethodData.iterationIndex;
    }

    /**
     * Setter to inject the id into the object after constructing it
     * 
     * @param id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    private String getId()
    {
        return StringUtils.isBlank(id) ? Thread.currentThread().hashCode() + "" : id;
    }

    private AllureData getAllureContext()
    {
        return ALLURE_CONTEXTS.computeIfAbsent(getId(), key -> {
            return new AllureData();
        });
    }

    /**
     * Method to call before every test method iteration. On first iteration it initializes ALLURE_CONTEXTS and on next
     * iterations it adjusts the history id and start time of the allure test case lifecycle to make the test be
     * correctly displayed in the report (in retries tab or in overview).
     * 
     * @return * true - if the test should not be executed * false - if test should be executed
     */
    public boolean isShouldBeSkipped()
    {
        // initializing Allure context.
        // Skipping this part if no Allure lifecycle is present, e.g. on JUnit 4 execution in Eclipse,
        // otherwise exception can be thrown
        if (iterationIndex == 0 && Allure.getLifecycle().getCurrentTestCase().isPresent())
        {
            Allure.getLifecycle().updateTestCase(r -> getAllureContext().setStart(r.getStart()));
            Allure.getLifecycle().updateTestCase(r -> getAllureContext().setHistoryId(r.getHistoryId()));
            Allure.getLifecycle().updateTestCase(r -> r.setHistoryId(getAllureContext().getHistoryId()));
        }
        boolean shouldBeSkipped = (ERROR_CONTEXTS.get(getId()) == null
                                   || !exceptions.isEmpty() && !exceptions.stream()
                                                                          .anyMatch(expExc -> (ERROR_CONTEXTS.get(getId())
                                                                                                             .getMessage() == null ? ""
                                                                                                                                   : ERROR_CONTEXTS.get(getId())
                                                                                                                                                   .getMessage())
                                                                                                                                                                 .contains(expExc)));

        // if this is not first run of the test method, adjust its allure properties to bind it to the first run
        if (iterationIndex > 0 && Allure.getLifecycle().getCurrentTestCase().isPresent())
        {
            // Unfortunately we cannot avoid Allure logging errors after the manipulations because we cannot prevent
            // Allure listeners from trying to adjust the test again, which leads to error logs
            // Therefore we can try to mute the Allure logs completely, the problem is though that it's only possible to
            // mute Allure logs with log4j but we don't want to have log4j as dependency to allow users to select the
            // logging service of their choice. This is how we can disable the logging with log4j:

            // Turning the logging back on
            // Configurator.setLevel("io.qameta.allure.AllureLifecycle", Level.INFO);
            Allure.getLifecycle().updateTestCase(r -> r.setTestCaseId(getAllureContext().getLabelId()));
            Allure.getLifecycle().updateTestCase(r -> r.setHistoryId(getAllureContext().getHistoryId()));

            // if the test should be skipped, stop it's allure lifecycle, mute it, mark as skipped and change the start
            // and stop times as if it ran before the actual execution to make it go to the retries tab
            if (shouldBeSkipped)
            {
                Allure.getLifecycle().updateTestCase(r -> r.setStart(getAllureContext().getStart() - 1));
                String uuidTest = Allure.getLifecycle().getCurrentTestCase().get();
                Allure.getLifecycle().stopTestCase(uuidTest);
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStage(Stage.FINISHED));
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStatus(Status.SKIPPED));
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStatusDetails(new StatusDetails().setMuted(true).setMessage("skip repetition")));
                Allure.getLifecycle().updateTestCase(uuidTest, r -> r.setStop(getAllureContext().getStart() - 1));
                Allure.getLifecycle().writeTestCase(uuidTest);

                // Turning the logging off
                // Configurator.setLevel("io.qameta.allure.AllureLifecycle", Level.FATAL);
            }
        }

        // if it's a retry that should be executed, mark test as flaky
        if (maxExecutions > 1 && iterationIndex > 0 && !shouldBeSkipped)
        {
            Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
        }

        // always execute the first run but skip the next onces if needed
        return iterationIndex > 0 && shouldBeSkipped;
    }

    /**
     * Method to call on test failure to decide what exception should be thrown.
     * 
     * @param throwable
     *            original error on which test failed
     * @return * original error if the test should not be repeated (unexpected error or last iteration) *
     *         {@link TestFailedAndShouldBeRetired} exception in case the execution should be ignored and next try
     *         should be executed
     */
    public Throwable handleTestExecutionException(Throwable throwable)
    {
        // if this is not first run of the test method, adjust its allure properties to bind it to the first run
        if (iterationIndex > 0 && Allure.getLifecycle().getCurrentTestCase().isPresent())
        {
            Allure.getLifecycle().updateTestCase(r -> r.setTestCaseId(getAllureContext().getLabelId()));
            Allure.getLifecycle().updateTestCase(r -> r.setHistoryId(getAllureContext().getHistoryId()));
        }

        // if it was the last iteration, clear the context
        if (iterationIndex == maxExecutions - 1)
        {
            ALLURE_CONTEXTS.remove(getId());
            ERROR_CONTEXTS.remove(getId());
        }
        // else put the error in context to keep it for the next iteration
        else
        {
            ERROR_CONTEXTS.put(getId(), throwable);
        }

        // if it's not the last iteration and the error that caused the failure is expected
        if (iterationIndex < maxExecutions - 1 && (throwable != null && (exceptions.isEmpty()
                                                                         || exceptions.stream()
                                                                                      .anyMatch(expExc -> (throwable.getMessage() == null ? ""
                                                                                                                                          : throwable.getMessage())
                                                                                                                                                                   .contains(expExc)))))
        {
            // mark test as flaky
            Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setFlaky(true)));
            // return exception that will cause the execution to be ignored
            return new TestFailedAndShouldBeRetired(iterationIndex + 1, throwable);
        }
        // if it's the last iteration or the error is not expected
        else
        {
            // return original error
            return throwable;
        }
    }

    /**
     * Method to call on test success to clear the context and mark that it should not be repeated
     */
    public void testSuccessful()
    {
        ERROR_CONTEXTS.remove(getId());
    }
}
