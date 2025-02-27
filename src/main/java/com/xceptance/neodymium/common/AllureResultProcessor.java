package com.xceptance.neodymium.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.StepResult;

public class AllureResultProcessor
{
    /**
     * Get the Allure attachments of the current test.
     *
     * @return List<Attachment> of the test
     */
    public static List<Attachment> getAllureResultAttachments()
    {
        List<Attachment> attachments = new LinkedList<>();

        AllureLifecycle lifecycle = Allure.getLifecycle();

        // parse all steps and get the attachments
        lifecycle.updateTestCase((result) -> {
            getAttachmentsFromResult(result, attachments);
        });

        return attachments;
    }

    public static List<Attachment> getAttachmentsFromResult(ExecutableItem result)
    {
        List<Attachment> attachments = new LinkedList<>();
        getAttachmentsFromResult(result, attachments);

        return attachments;
    }

    public static void getAttachmentsFromResult(ExecutableItem result, List<Attachment> attachments)
    {
        List<Attachment> customAttachments = result.getAttachments();
        attachments.addAll(customAttachments);

        var steps = result.getSteps();

        for (var step : steps)
        {
            attachments.addAll(traverseSteps(step));
        }
    }

    /**
     * Traverse all steps and get their attachments.
     *
     * @param step
     *            the base/root step of the AllureLifecycle
     * @return List<Attachment> of the steps
     */
    public static List<Attachment> traverseSteps(StepResult step)
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
