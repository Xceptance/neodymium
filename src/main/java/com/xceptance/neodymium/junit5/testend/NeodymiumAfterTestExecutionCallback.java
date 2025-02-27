package com.xceptance.neodymium.junit5.testend;

import static com.xceptance.neodymium.common.AllureResultProcessor.getAllureResultAttachments;

import java.util.List;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.xceptance.neodymium.common.ScreenshotWriter;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Attachment;

public class NeodymiumAfterTestExecutionCallback implements AfterTestExecutionCallback
{
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception
    {
        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), context.getExecutionException(),
                                      context.getRequiredTestMethod().getAnnotations());

        // TODO instead use the listeners AllureTestLifecycleListener and AllureContainerLifecycleListener to process the Allure
        // results
        // TODO to make them work, the files
        // src/main/resources/META-INF/services/io.qameta.allure.listener.ContainerLifecycleListener and
        // src/main/resources/META-INF/services/io.qameta.allure.listener.TestLifecycleListener
        // must be added to the test resources directory in the test project

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
}
