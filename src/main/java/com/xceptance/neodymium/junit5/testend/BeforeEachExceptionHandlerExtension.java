package com.xceptance.neodymium.junit5.testend;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;

import com.xceptance.neodymium.common.ScreenshotWriter;
import com.xceptance.neodymium.common.TestStage;

public class BeforeEachExceptionHandlerExtension implements LifecycleMethodExecutionExceptionHandler
{

//    @Override
//    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
//    {
//        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), Optional.of(throwable),
//                                      context.getRequiredTestMethod().getAnnotations(), TestStage.BEFORE_EACH);
//
//        LifecycleMethodExecutionExceptionHandler.super.handleBeforeEachMethodExecutionException(context, throwable);
//    }
//
//    @Override
//    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
//    {
//        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), Optional.of(throwable),
//                                      context.getRequiredTestMethod().getAnnotations(), TestStage.AFTER_EACH);
//
//        LifecycleMethodExecutionExceptionHandler.super.handleAfterEachMethodExecutionException(context, throwable);
//    }
//    
//    @Override
//    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
//    {
//        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), Optional.of(throwable),
//                                      context.getRequiredTestMethod().getAnnotations(), TestStage.AFTER_ALL);
//
//        LifecycleMethodExecutionExceptionHandler.super.handleAfterAllMethodExecutionException(context, throwable);
//
//    }
//    
//    @Override
//    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
//    {
//        ScreenshotWriter.doScreenshot(context.getRequiredTestMethod().getName(), context.getRequiredTestClass().getName(), Optional.of(throwable),
//                                      context.getRequiredTestMethod().getAnnotations(), TestStage.BEFORE_ALL);
//
//        LifecycleMethodExecutionExceptionHandler.super.handleBeforeAllMethodExecutionException(context, throwable);
//    }
}
