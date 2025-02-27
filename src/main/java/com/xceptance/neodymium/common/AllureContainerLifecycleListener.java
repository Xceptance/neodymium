package com.xceptance.neodymium.common;

import static com.xceptance.neodymium.common.AllureResultProcessor.getAttachmentsFromResult;

import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.listener.ContainerLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.TestResultContainer;

// this is most likely the best way to process the allure results for the container

// TODO to make them work, the files
// src/main/resources/META-INF/services/io.qameta.allure.listener.ContainerLifecycleListener and
// src/main/resources/META-INF/services/io.qameta.allure.listener.TestLifecycleListener
// must be added to the test resources directory in the test project
public class AllureContainerLifecycleListener implements ContainerLifecycleListener
{
    @Override
    public void beforeContainerStart(TestResultContainer container)
    {
        ContainerLifecycleListener.super.beforeContainerStart(container);
    }

    @Override
    public void afterContainerStart(TestResultContainer container)
    {
        ContainerLifecycleListener.super.afterContainerStart(container);
    }

    @Override
    public void beforeContainerUpdate(TestResultContainer container)
    {
        ContainerLifecycleListener.super.beforeContainerUpdate(container);
    }

    @Override
    public void afterContainerUpdate(TestResultContainer container)
    {
        ContainerLifecycleListener.super.afterContainerUpdate(container);
    }

    @Override
    public void beforeContainerStop(TestResultContainer container)
    {
        ContainerLifecycleListener.super.beforeContainerStop(container);
    }

    @Override
    public void afterContainerStop(TestResultContainer container)
    {
        ContainerLifecycleListener.super.afterContainerStop(container);
    }

    @Override
    public void beforeContainerWrite(TestResultContainer container)
    {
        ContainerLifecycleListener.super.beforeContainerWrite(container);
    }

    @Override
    public void afterContainerWrite(TestResultContainer container)
    {
        ContainerLifecycleListener.super.afterContainerWrite(container);

        List<FixtureResult> beforeResults = container.getBefores();
        List<FixtureResult> afterResults = container.getAfters();

        List<Attachment> beforeAttachments = new LinkedList<>();
        for (FixtureResult result : beforeResults)
        {
            beforeAttachments.addAll(getAttachmentsFromResult(result));
        }

        List<Attachment> afterAttachments = new LinkedList<>();
        for (FixtureResult result : afterResults)
        {
            afterAttachments.addAll(getAttachmentsFromResult(result));
        }

        // TODO process attachments instead of just print them

        System.out.println("container before attachments:");
        System.out.println(beforeAttachments);

        for (Attachment attachment : beforeAttachments)
        {
            System.out.println(attachment.getSource());
        }

        System.out.println("container after attachments:");
        System.out.println(afterAttachments);

        for (Attachment attachment : afterAttachments)
        {
            System.out.println(attachment.getSource());
        }
    }
}
