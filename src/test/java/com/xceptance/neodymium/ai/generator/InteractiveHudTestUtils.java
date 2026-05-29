/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.ai.generator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

import org.openqa.selenium.WebDriver;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.core.AiAgent;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.core.LlmClient;
import com.xceptance.neodymium.ai.core.PageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Utility helper class for Neodymium AI Interactive HUD integration tests.
 * Provides helper methods for executing background threads with shared Neodymium and WebDriver
 * context, creating mock LLM clients, and building custom test AiBrowser instances.
 *
 * // AI-generated: Antigravity (Gemini 2.5 Pro)
 */
public final class InteractiveHudTestUtils
{
    /**
     * Private constructor to prevent instantiation.
     */
    private InteractiveHudTestUtils()
    {
    }

    /**
     * Executes the given task in a background thread while copying and maintaining the main thread's
     * Neodymium thread-local context and WebDriver session.
     *
     * @param task the background task to run
     * @param onErrorCallback the callback to execute when the background thread encounters a throwable
     * @return the started background thread
     */
    public static Thread runInteractiveInBg(final Runnable task, final Function<Throwable, Void> onErrorCallback)
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final Object mainContext;
        try
        {
            mainContext = getNeodymiumContext();
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }

        final Thread bgThread = new Thread(() ->
        {
            try
            {
                setNeodymiumContext(mainContext);
                WebDriverRunner.setWebDriver(driver);
                task.run();
            }
            catch (final Throwable e)
            {
                onErrorCallback.apply(e);
            }
        });
        bgThread.start();
        return bgThread;
    }

    /**
     * Reflectively retrieves the current Neodymium ThreadLocal context object.
     *
     * @return the Neodymium context object
     * @throws Exception if reflection fails
     */
    public static Object getNeodymiumContext() throws Exception
    {
        final Method getContextMethod = Neodymium.class.getDeclaredMethod("getContext");
        getContextMethod.setAccessible(true);
        return getContextMethod.invoke(null);
    }

    /**
     * Reflectively injects the given Neodymium context object for the calling thread.
     *
     * @param context the context to inject
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    public static void setNeodymiumContext(final Object context) throws Exception
    {
        final Field field = Neodymium.class.getDeclaredField("CONTEXTS");
        field.setAccessible(true);
        final Map<Thread, Object> contexts = (Map<Thread, Object>) field.get(null);
        contexts.put(Thread.currentThread(), context);
    }

    /**
     * Creates a custom AiBrowser instance injected with a mock LLM agent that handles prompts with the given chat handler.
     *
     * @param testInstance the test class instance running the test
     * @param chatHandler the function handling the mock LLM prompts
     * @return the configured AiBrowser instance
     * @throws Exception if injection fails
     */
    public static AiBrowser createTestAiBrowser(final Object testInstance, final Function<String, String> chatHandler) throws Exception
    {
        final LlmClient mockLlmClient = new MockLlmClient(chatHandler);
        return createTestAiBrowser(testInstance, mockLlmClient);
    }

    /**
     * Creates a custom AiBrowser instance injected with the given custom mock LLM client.
     *
     * @param testInstance the test class instance running the test
     * @param mockLlmClient the custom mock LLM client
     * @return the configured AiBrowser instance
     * @throws Exception if injection fails
     */
    public static AiBrowser createTestAiBrowser(final Object testInstance, final LlmClient mockLlmClient) throws Exception
    {
        final AiBrowser browser = new AiBrowser(testInstance);
        final AiAgent customAgent = new AiAgent(
            mockLlmClient,
            new PageAnalyzer(),
            new ActionExecutor(testInstance),
            Neodymium.aiConfiguration()
        );
        final Field agentField = AiBrowser.class.getDeclaredField("agent");
        agentField.setAccessible(true);
        agentField.set(browser, customAgent);
        return browser;
    }

    /**
     * Mock LlmClient implementation that delegates chat requests to a custom handler function.
     */
    public static final class MockLlmClient extends LlmClient
    {
        private final Function<String, String> chatHandler;

        /**
         * Constructs a MockLlmClient with the specified chat handler.
         *
         * @param chatHandler the function handling LLM chat prompts
         */
        public MockLlmClient(final Function<String, String> chatHandler)
        {
            super(Neodymium.aiConfiguration(), new AiStats());
            this.chatHandler = chatHandler;
        }

        @Override
        public String chat(final String systemPrompt, final String userPrompt)
        {
            return chatHandler.apply(userPrompt);
        }

        @Override
        public String chatWithScreenshot(final String systemPrompt, final String userPrompt, final String screenshot)
        {
            return chatHandler.apply(userPrompt);
        }
    }
}
