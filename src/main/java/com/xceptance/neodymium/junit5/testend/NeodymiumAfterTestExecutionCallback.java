package com.xceptance.neodymium.junit5.testend;

import com.xceptance.neodymium.common.ScreenshotWriter;
import com.xceptance.neodymium.util.Neodymium;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class NeodymiumAfterTestExecutionCallback implements AfterTestExecutionCallback
{
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception
    {
        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), context.getExecutionException(),
                                      context.getRequiredTestMethod().getAnnotations());

        // TODO this processes the attachments from the actual test. If the test fails in the before or after the attachments are NOT processed.
        // to do so, the corresponding callbacks need to be extended. Also the attachments there seam to not be part of the lifecycle but of the container instead.
        processAllureResultAttachments();
    }

    /**
     * Get the current test ID, store it in {@link Neodymium#testIds} and process the attachments.
     */
    private void processAllureResultAttachments()
    {
        String testId = Allure.getLifecycle().getCurrentTestCase().orElseGet(() -> "no current test case");

        // get the test ID and store it to process it after the test is finished and the Allure results are written
        Neodymium.testIds.put(testId, testId);

        // process the attachments
        List<Attachment> attachments = getAllureResultAttachments();

        // TODO implement processing steps
    }

    /**
     * Get the Allure attachments of the current test.
     *
     * @return List<Attachment> of the test
     */
    private List<Attachment> getAllureResultAttachments()
    {
        List<Attachment> attachments = new LinkedList<>();

        AllureLifecycle lifecycle = Allure.getLifecycle();

        // parse all steps and get the attachments
        lifecycle.updateTestCase((result) -> {
            List<Attachment> customAttachments = result.getAttachments();
            attachments.addAll(customAttachments);

            var steps = result.getSteps();

            for (var step : steps)
            {
                attachments.addAll(traverseSteps(step));
            }
        });

        return attachments;
    }

    /**
     * Traverse all steps and get their attachments.
     *
     * @param step
     *     the base/root step of the AllureLifecycle
     * @return List<Attachment> of the steps
     */
    private List<Attachment> traverseSteps(StepResult step)
    {
        List<Attachment> attachments = new ArrayList<>();
        Deque<StepResult> stepDeque = new ArrayDeque<>();
        stepDeque.add(step);

        // process all steps
        while (!stepDeque.isEmpty())
        {
            // process the current step
            StepResult currentStep = stepDeque.poll();
            // get the attachments
            attachments.addAll(currentStep.getAttachments());

            // add all sibling steps
            currentStep.getSteps().forEach(stepDeque::push);
        }
        return attachments;
    }
}
