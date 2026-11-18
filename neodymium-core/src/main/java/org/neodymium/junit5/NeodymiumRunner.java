/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.junit5;

import com.codeborne.selenide.logevents.SelenideLogger;
import com.google.common.collect.ImmutableMap;
import org.neodymium.common.TestStepListener;
import org.neodymium.util.AllureAddons;
import org.neodymium.util.AllureAddons.EnvironmentInfoMode;
import org.neodymium.util.Neodymium;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.neodymium.util.NeodymiumAnnotationUtils;

public class NeodymiumRunner implements TestTemplateInvocationContextProvider
{

    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumRunner.class);

    public static final String LISTENER_NAME = "allure-selenide-java";

    private static boolean neoVersionLogged = false;

    private NeodymiumData neoData;

    public NeodymiumRunner()
    {
        AllureSelenide allureSelenide = new AllureSelenide();

        // if advanced screenshots are enabled, Selenide screenshots should be disabled
        allureSelenide.screenshots(!Neodymium.configuration().enableAdvancedScreenShots());

        SelenideLogger.addListener(LISTENER_NAME, allureSelenide);
        SelenideLogger.addListener(TestStepListener.LISTENER_NAME, new TestStepListener());

        if (!neoVersionLogged && Neodymium.configuration().logNeoVersion())
        {
            if (!AllureAddons.envFileExists())
            {
                LOGGER.info("This test uses Neodymium Library (version: " + Neodymium.getNeodymiumVersion()
                            + "), MIT License, more details on https://github.com/Xceptance/neodymium");
                neoVersionLogged = true;
                AllureAddons.addEnvironmentInformation(ImmutableMap.<String, String> builder()
                                                                   .put("Testing Framework", "Neodymium " + Neodymium.getNeodymiumVersion())
                                                                   .build(),
                                                       EnvironmentInfoMode.IGNORE);
            }
        }
        AllureAddons.initializeEnvironmentInformation();
    }

    public enum DescriptionMode
    {
        flat,
        tree,
    }

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context)
    {
        if (context.getTestMethod().isEmpty())
        {
            return false;
        }
        final Class<?> testClass = context.getRequiredTestClass();
        final Method method = context.getRequiredTestMethod();
        if (NeodymiumAnnotationUtils.isAnnotationPresent(testClass, "NeodymiumAiTest")
            || NeodymiumAnnotationUtils.isAnnotationPresent(testClass, "AiPlaybook")
            || NeodymiumAnnotationUtils.isAnnotationPresent(method, "AiPlaybook"))
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean mayReturnZeroTestTemplateInvocationContexts(ExtensionContext context)
    {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context)
    {
        // clear the context before next child run
        Neodymium.clearThreadContext();

        Class<?> testClass = context.getRequiredTestClass();
        Method templateMethod = context.getRequiredTestMethod();
        if (neoData == null)
        {
            neoData = new NeodymiumData(testClass);
        }
        return neoData.computeTestMethods(templateMethod);
    }
}
