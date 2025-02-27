package com.xceptance.neodymium.common;

import static com.xceptance.neodymium.common.AllureResultProcessor.getAttachmentsFromResult;

import java.util.List;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;

// this is most likely the best way to process the allure results for the tests

// TODO to make them work, the files
// src/main/resources/META-INF/services/io.qameta.allure.listener.ContainerLifecycleListener and
// src/main/resources/META-INF/services/io.qameta.allure.listener.TestLifecycleListener
// must be added to the test resources directory in the test project
public class AllureTestLifecycleListener implements TestLifecycleListener
{
    @Override
    public void beforeTestSchedule(TestResult result)
    {
        TestLifecycleListener.super.beforeTestSchedule(result);
    }

    @Override
    public void afterTestSchedule(TestResult result)
    {
        TestLifecycleListener.super.afterTestSchedule(result);
    }

    @Override
    public void beforeTestUpdate(TestResult result)
    {
        TestLifecycleListener.super.beforeTestUpdate(result);
    }

    @Override
    public void afterTestUpdate(TestResult result)
    {
        TestLifecycleListener.super.afterTestUpdate(result);
    }

    @Override
    public void beforeTestStart(TestResult result)
    {
        TestLifecycleListener.super.beforeTestStart(result);
    }

    @Override
    public void afterTestStart(TestResult result)
    {
        TestLifecycleListener.super.afterTestStart(result);
    }

    @Override
    public void beforeTestStop(TestResult result)
    {
        TestLifecycleListener.super.beforeTestStop(result);
    }

    @Override
    public void afterTestStop(TestResult result)
    {
        TestLifecycleListener.super.afterTestStop(result);
    }

    @Override
    public void beforeTestWrite(TestResult result)
    {
        TestLifecycleListener.super.beforeTestWrite(result);
    }

    @Override
    public void afterTestWrite(TestResult result)
    {
        TestLifecycleListener.super.afterTestWrite(result);

        List<Attachment> attachments = getAttachmentsFromResult(result);

        // TODO process attachments instead of just print them

        System.out.println("test attachments:");
        System.out.println(attachments);

        for (Attachment attachment : attachments)
        {
            System.out.println(attachment.getSource());
        }
    }
}
