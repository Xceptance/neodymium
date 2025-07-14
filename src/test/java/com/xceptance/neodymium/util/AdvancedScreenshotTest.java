package com.xceptance.neodymium.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.ScreenshotWriter;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumRunner;
import com.xceptance.neodymium.junit5.NeodymiumTest;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.StepResult;

@ExtendWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class AdvancedScreenshotTest
{
    @NeodymiumTest
    public void testFullPageScreenshot() throws IOException
    {
        Selenide.open("https://www.xceptance.com/en/");

        // take not blurred screenshot
        try
        {
            Allure.step("test", () -> ScreenshotWriter.doScreenshot("blurredScreenshot"));
        }
        catch (Exception e)
        {
            // intentionally blank
        }

        // take blurred screenshot
        Neodymium.configuration().setProperty("neodymium.screenshots.blurFullPageScreenshot", "true");
        try
        {
            Allure.step("test", () -> ScreenshotWriter.doScreenshot("blurredScreenshot"));
        }
        catch (Exception e)
        {
            // intentionally blank
        }

        // compare them
        List<Attachment> attachments = getAllureResultAttachments();

        Assertions.assertFalse(compareImage(new File("allure-results/" + attachments.get(0).getSource()),
                                            new File("allure-results/" + attachments.get(1).getSource())),
                               "blurred and not blurred images should not be the same");
    }

    private static List<Attachment> getAllureResultAttachments()
    {
        List<Attachment> attachments = new LinkedList<>();

        AllureLifecycle lifecycle = Allure.getLifecycle();

        // parse all steps and get the attachments
        lifecycle.updateTestCase((result) -> {
            getAttachmentsFromResult(result, attachments);
        });

        return attachments;
    }

    private static void getAttachmentsFromResult(ExecutableItem result, List<Attachment> attachments)
    {
        List<Attachment> customAttachments = result.getAttachments();
        attachments.addAll(customAttachments);

        var steps = result.getSteps();

        for (var step : steps)
        {
            attachments.addAll(traverseSteps(step));
        }
    }

    private static List<Attachment> traverseSteps(StepResult step)
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

    private static boolean compareImage(File actual, File expected) throws IOException
    {
        BufferedImage actualBuffered = ImageIO.read(actual);
        BufferedImage expectedBuffered = ImageIO.read(expected);

        DataBuffer dbActual = actualBuffered.getRaster().getDataBuffer();
        DataBuffer dbExpected = expectedBuffered.getRaster().getDataBuffer();

        DataBufferByte actualBufferedByte = (DataBufferByte) dbActual;
        DataBufferByte expectedBufferedByte = (DataBufferByte) dbExpected;

        boolean equal = true;
        for (int bank = 0; bank < actualBufferedByte.getNumBanks(); bank++)
        {
            byte[] actualArray = actualBufferedByte.getData(bank);
            byte[] expectedArray = expectedBufferedByte.getData(bank);

            // this line may vary depending on your test framework
            equal = Arrays.equals(actualArray, expectedArray);

            if (!equal)
            {
                break;
            }
        }

        return equal;
    }
}
