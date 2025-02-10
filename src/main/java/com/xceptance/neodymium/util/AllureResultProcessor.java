package com.xceptance.neodymium.util;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.File;
import java.util.Optional;

// The AllureLifecycle only yields a test name if the current test is still running but then the result isn't written yet.
// To solve this the ID and the attachments are grabbed directly after the run by NeodymiumAfterTestExecutionCallback and stored in Neodymium.
// This class is executed by surefire after each test run, especially after Allure is done, so the results were written and can be processed here.

public class AllureResultProcessor implements TestExecutionListener
{
    // TODO src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener must be added to the same directory of the test project

    /**
     * Process the Allure test results after they are written.
     *
     * @param testIdentifier
     *     – the identifier of the finished test or container testExecutionResult
     * @param testExecutionResult
     *     - the (unaggregated) result of the execution for the supplied TestIdentifier
     */
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult)
    {
        // get the status which can be used if needed
        TestExecutionResult.Status status = testExecutionResult.getStatus();

        // process the stored test ID
        Optional<String> storedTestId = Neodymium.testIds.keySet().stream().findFirst();

        // safety check. if no test ID is stored fail fast
        if (storedTestId.isEmpty())
        {
            return;
        }

        // process the results file if it is present
        if (new File("./target/allure-results/" + storedTestId.get() + "-result.json").isFile())
        {
            Neodymium.testIds.remove(storedTestId.get());

            // TODO process the test results
        }
    }
}
