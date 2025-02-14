package com.xceptance.neodymium.junit5.testend;

import com.xceptance.neodymium.common.ScreenshotWriter;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class NeodymiumAfterTestExecutionCallback implements AfterTestExecutionCallback
{
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception
    {
        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), context.getExecutionException(),
                                      context.getRequiredTestMethod().getAnnotations());
    }
}
