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
package org.neodymium.ai.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Unit tests for {@link LlmCommunicationLogger}.
 * Validates default disabled state, configuration-based activation, Log4j2 level activation,
 * and separate file output dispatching.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class LlmCommunicationLoggerTest
{
    private static final String LOG_FILE_PATH = "target/neodymium-ai-communication.log";

    @BeforeEach
    public void setUp()
    {
        System.clearProperty("neodymium.ai.communicationLog.enabled");
        System.clearProperty("neodymium.ai.communicationLog");
        System.clearProperty("neodymium.ai.communication.log");
        System.clearProperty("neodymium.ai.openai.apiKey");
        System.clearProperty("neodymium.ai.mistral.apiKey");
        System.clearProperty("neodymium.ai.vertex.apiKey");
        System.clearProperty("neodymium.ai.vertex.projectId");
        System.clearProperty("neodymium.ai.gemini.apiKey");
        AiConfiguration.resetInstance();
        Configurator.setLevel(LlmCommunicationLogger.LOGGER_NAME, Level.OFF);
    }

    @AfterEach
    public void tearDown()
    {
        System.clearProperty("neodymium.ai.communicationLog.enabled");
        System.clearProperty("neodymium.ai.communicationLog");
        System.clearProperty("neodymium.ai.communication.log");
        System.clearProperty("neodymium.ai.openai.apiKey");
        System.clearProperty("neodymium.ai.mistral.apiKey");
        System.clearProperty("neodymium.ai.vertex.apiKey");
        System.clearProperty("neodymium.ai.vertex.projectId");
        System.clearProperty("neodymium.ai.gemini.apiKey");
        AiConfiguration.resetInstance();
        Configurator.setLevel(LlmCommunicationLogger.LOGGER_NAME, Level.OFF);
    }

    @Test
    public void testDisabledByDefault()
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        assertFalse(config.isCommunicationLogEnabled(), "Communication log should be disabled by default.");
        assertFalse(LlmCommunicationLogger.isLoggingActive(), "Communication logger should not be active by default.");
    }

    @Test
    public void testEnabledViaAiConfiguration()
    {
        System.setProperty("neodymium.ai.communicationLog.enabled", "true");
        AiConfiguration.resetInstance();

        final AiConfiguration config = AiConfiguration.getInstance();
        assertTrue(config.isCommunicationLogEnabled(), "Communication log should be enabled via property.");
        assertTrue(LlmCommunicationLogger.isLoggingActive(), "Communication logger should become active.");
        assertNotNull(LlmCommunicationLogger.getLogger(), "Logger instance should not be null.");
    }

    @Test
    public void testEnabledViaAlternativePropertyKeys()
    {
        System.setProperty("neodymium.ai.communicationLog", "true");
        AiConfiguration.resetInstance();
        assertTrue(LlmCommunicationLogger.isLoggingActive(), "Communication log should be enabled via 'neodymium.ai.communicationLog'.");

        System.clearProperty("neodymium.ai.communicationLog");
        System.setProperty("neodymium.ai.communication.log", "true");
        AiConfiguration.resetInstance();
        assertTrue(LlmCommunicationLogger.isLoggingActive(), "Communication log should be enabled via 'neodymium.ai.communication.log'.");
    }

    @Test
    public void testEnabledViaLog4jLevel()
    {
        assertFalse(LlmCommunicationLogger.isLoggingActive());

        Configurator.setLevel(LlmCommunicationLogger.LOGGER_NAME, Level.INFO);
        assertTrue(LlmCommunicationLogger.isLoggingActive(), "Communication logger should be active when Log4j level is INFO.");

        Configurator.setLevel(LlmCommunicationLogger.LOGGER_NAME, Level.OFF);
        assertFalse(LlmCommunicationLogger.isLoggingActive(), "Communication logger should be inactive when Log4j level is OFF.");
    }

    @Test
    public void testMockProviderLogsToFileWhenEnabled() throws IOException
    {
        final File logFile = new File(LOG_FILE_PATH);
        if (logFile.exists())
        {
            Files.writeString(logFile.toPath(), "");
        }

        System.setProperty("neodymium.ai.communicationLog.enabled", "true");
        AiConfiguration.resetInstance();

        final MockLlmProvider mock = new MockLlmProvider();
        mock.addResponse(new LlmResponse("Hello from test mock", null, "mock-model"));

        final LlmRequest request = new LlmRequest(null, "Test prompt message", List.of(), ResponseSchema.TEXT, 0.0, 30);
        final LlmResponse response = mock.chat(request);

        assertNotNull(response);
        assertTrue(logFile.exists(), "Log file should exist after logging while active.");

        final String logContent = Files.readString(logFile.toPath());
        assertTrue(logContent.contains("Mock LLM request:"), "Log file should contain request entries.");
        assertTrue(logContent.contains("Test prompt message"), "Log file should contain the sent prompt.");
        assertTrue(logContent.contains("Mock LLM response:"), "Log file should contain response entries.");
        assertTrue(logContent.contains("Hello from test mock"), "Log file should contain the response text.");
    }

    @Test
    public void testMockProviderDoesNotLogWhenDisabled() throws IOException
    {
        final File logFile = new File(LOG_FILE_PATH);
        if (logFile.exists())
        {
            Files.writeString(logFile.toPath(), "");
        }

        final MockLlmProvider mock = new MockLlmProvider();
        mock.addResponse(new LlmResponse("Disabled response", null, "mock-model"));

        final LlmRequest request = new LlmRequest(null, "Silent prompt", List.of(), ResponseSchema.TEXT, 0.0, 30);
        final LlmResponse response = mock.chat(request);

        assertNotNull(response);
        if (logFile.exists())
        {
            final String logContent = Files.readString(logFile.toPath());
            assertFalse(logContent.contains("Silent prompt"), "Log file should not receive entries when disabled.");
        }
    }

    @Test
    public void testExplicitFalseOverridesAliasTrue()
    {
        System.setProperty("neodymium.ai.communicationLog.enabled", "false");
        System.setProperty("neodymium.ai.communicationLog", "true");
        AiConfiguration.resetInstance();

        final AiConfiguration config = AiConfiguration.getInstance();
        assertFalse(config.isCommunicationLogEnabled(), "Explicit .enabled=false must take precedence over alias.");
        assertFalse(LlmCommunicationLogger.isLoggingActive(), "Communication logger must remain inactive.");
    }

    @Test
    public void testProvidersConstructWithoutErrorWhenLoggingActive()
    {
        System.setProperty("neodymium.ai.communicationLog.enabled", "true");
        System.setProperty("neodymium.ai.openai.apiKey", "test-key");
        System.setProperty("neodymium.ai.mistral.apiKey", "test-key");
        System.setProperty("neodymium.ai.vertex.apiKey", "test-key");
        System.setProperty("neodymium.ai.vertex.projectId", "test-project");
        System.setProperty("neodymium.ai.gemini.apiKey", "test-key");
        AiConfiguration.resetInstance();

        assertTrue(LlmCommunicationLogger.isLoggingActive());

        // Ensure instantiation of providers succeeds with communication logging enabled
        final OpenAiLlmProvider openAi = new OpenAiLlmProvider();
        assertNotNull(openAi);

        final MistralLlmProvider mistral = new MistralLlmProvider();
        assertNotNull(mistral);

        final VertexAiLlamaProvider vertex = new VertexAiLlamaProvider();
        assertNotNull(vertex);

        final GeminiLlmProvider gemini = new GeminiLlmProvider();
        assertNotNull(gemini);
    }
}
