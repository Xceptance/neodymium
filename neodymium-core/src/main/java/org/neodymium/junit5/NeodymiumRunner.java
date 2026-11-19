/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

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
        if (testClass.isAnnotationPresent(NeodymiumAiTest.class)
            || testClass.isAnnotationPresent(AiPlaybook.class)
            || method.isAnnotationPresent(AiPlaybook.class))
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
