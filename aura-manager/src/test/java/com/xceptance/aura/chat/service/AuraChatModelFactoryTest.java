/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.chat.service;

import com.google.genai.types.HttpOptions;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neodymium.ai.config.AiConfiguration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

/**
 * Unit tests for {@link AuraChatModelFactory}.
 *
 * @author AI-generated: Claude Sonnet 4.6
 * @author Xceptance GmbH 2026
 */
public class AuraChatModelFactoryTest
{
    @Test
    public void testOpenAiProviderDoesNotInheritGeminiDefaultModel()
    {
        final AiConfiguration config = mockConfig(Map.of(
                "neodymium.ai.provider", "gemini",
                "neodymium.ai.model", "gemini-2.5-flash",
                "neodymium.ai.chat.provider", "openai",
                "neodymium.ai.openai.apiKey", "sk-test-openai"
        ));
        Mockito.when(config.getProvider("chat")).thenReturn("openai");
        Mockito.when(config.getModel("chat")).thenReturn("gemini-2.5-flash");
        Mockito.when(config.getTemperature("chat")).thenReturn(0.2);

        final AuraChatModelFactory factory = new AuraChatModelFactory(key -> null);
        final String resolvedModel = factory.resolveModelName(config, "openai");
        final String resolvedKey = factory.resolveApiKey(config, "openai");

        Assertions.assertNull(resolvedModel);
        Assertions.assertEquals("sk-test-openai", resolvedKey);

        final ChatModel chatModel = factory.createChatModel(config);
        Assertions.assertInstanceOf(OpenAiChatModel.class, chatModel);
    }

    @Test
    public void testMistralProviderUsesExplicitModelAndEnvFallbackWithoutGeminiLeak()
    {
        final AiConfiguration config = mockConfig(Map.of(
                "neodymium.ai.provider", "gemini",
                "neodymium.ai.apiKey", "gemini-key-unused",
                "neodymium.ai.chat.provider", "mistral",
                "neodymium.ai.mistral.model", "mistral-large-latest"
        ));
        Mockito.when(config.getProvider("chat")).thenReturn("mistral");
        Mockito.when(config.aiApiKey()).thenReturn("gemini-key-unused");
        Mockito.when(config.getTemperature("chat")).thenReturn(0.1);

        final AuraChatModelFactory factory = new AuraChatModelFactory(
                env -> "MISTRAL_API_KEY".equals(env) ? "mistral-env-secret" : null);

        Assertions.assertEquals("mistral-large-latest", factory.resolveModelName(config, "mistral"));
        Assertions.assertEquals("mistral-env-secret", factory.resolveApiKey(config, "mistral"));

        final ChatModel chatModel = factory.createChatModel(config);
        Assertions.assertInstanceOf(MistralAiChatModel.class, chatModel);
    }

    @Test
    public void testModelInstanceIsCachedUntilConfigurationChanges()
    {
        final Map<String, String> props = new HashMap<>();
        props.put("neodymium.ai.chat.provider", "gemini");
        props.put("neodymium.ai.chat.apiKey", "gem-key-1");
        props.put("neodymium.ai.chat.model", "gemini-2.5-flash");

        final AiConfiguration config = mockConfig(props);
        Mockito.when(config.getProvider("chat")).thenReturn("gemini");
        Mockito.when(config.getTemperature("chat")).thenReturn(0.0);

        final AuraChatModelFactory factory = new AuraChatModelFactory(key -> null);
        final ChatModel first = factory.createChatModel(config);
        final ChatModel second = factory.createChatModel(config);

        Assertions.assertInstanceOf(GoogleGenAiChatModel.class, first);
        Assertions.assertSame(first, second);

        props.put("neodymium.ai.chat.model", "gemini-2.5-pro");
        final ChatModel third = factory.createChatModel(config);
        Assertions.assertNotSame(first, third);
    }

    @Test
    public void testUnsupportedProviderAndMissingKeyThrowIllegalStateException()
    {
        final AiConfiguration unsupportedConfig = mockConfig(Map.of(
                "neodymium.ai.chat.apiKey", "some-key"
        ));
        Mockito.when(unsupportedConfig.getProvider("chat")).thenReturn("anthropic");
        Mockito.when(unsupportedConfig.getTemperature("chat")).thenReturn(0.0);

        final AuraChatModelFactory factory = new AuraChatModelFactory(key -> null);
        Assertions.assertThrows(IllegalStateException.class, () -> factory.createChatModel(unsupportedConfig));

        final AiConfiguration missingKeyConfig = mockConfig(Map.of());
        Mockito.when(missingKeyConfig.getProvider("chat")).thenReturn("openai");
        Mockito.when(missingKeyConfig.getTemperature("chat")).thenReturn(0.0);

        final IllegalStateException missingEx = Assertions.assertThrows(
                IllegalStateException.class, () -> factory.createChatModel(missingKeyConfig));
        Assertions.assertTrue(missingEx.getMessage().contains("OPENAI_API_KEY"));

        final AiConfiguration geminiGlobalOpenAiChatConfig = mockConfig(Map.of(
                "neodymium.ai.provider", "gemini",
                "neodymium.ai.apiKey", "gemini-global-secret",
                "neodymium.ai.chat.provider", "openai"
        ));
        Mockito.when(geminiGlobalOpenAiChatConfig.getProvider("chat")).thenReturn("openai");
        Mockito.when(geminiGlobalOpenAiChatConfig.aiApiKey()).thenReturn("gemini-global-secret");
        Mockito.when(geminiGlobalOpenAiChatConfig.getTemperature("chat")).thenReturn(0.0);

        final IllegalStateException isolatedKeyEx = Assertions.assertThrows(
                IllegalStateException.class, () -> factory.createChatModel(geminiGlobalOpenAiChatConfig));
        Assertions.assertTrue(isolatedKeyEx.getMessage().contains("OPENAI_API_KEY"));
    }

    @Test
    public void testGeminiTimeoutAppliedAndCacheBounded()
    {
        final HttpOptions opts = AuraChatModelFactory.buildGeminiHttpOptions(
                Duration.ofSeconds(45), "https://custom.gemini.endpoint");
        Assertions.assertEquals(45_000, opts.timeout().orElseThrow());
        Assertions.assertEquals("https://custom.gemini.endpoint", opts.baseUrl().orElseThrow());

        final HttpOptions defaultOpts = AuraChatModelFactory.buildGeminiHttpOptions(Duration.ofSeconds(180), null);
        Assertions.assertEquals(180_000, defaultOpts.timeout().orElseThrow());
        Assertions.assertTrue(defaultOpts.baseUrl().isEmpty());

        final Map<String, String> props = new HashMap<>();
        props.put("neodymium.ai.chat.provider", "gemini");
        props.put("neodymium.ai.chat.apiKey", "gem-key-cache-test");
        final AiConfiguration config = mockConfig(props);
        Mockito.when(config.getProvider("chat")).thenReturn("gemini");
        Mockito.when(config.getTimeoutSeconds("chat")).thenReturn(45);
        Mockito.when(config.getTemperature("chat")).thenReturn(0.0);

        final AuraChatModelFactory factory = new AuraChatModelFactory(key -> null);
        for (int i = 0; i < 24; i++)
        {
            props.put("neodymium.ai.chat.model", "gemini-model-" + i);
            Assertions.assertInstanceOf(GoogleGenAiChatModel.class, factory.createChatModel(config));
        }

        Assertions.assertTrue(factory.getCacheSize() <= 16,
                "Model cache must remain bounded (LRU) to avoid unbounded heap growth");
        factory.closeCachedClients();
        Assertions.assertEquals(0, factory.getCacheSize());
    }

    @Test
    public void testProviderScopedChatApiKeyTakesPrecedenceOverGenericChatApiKey()
    {
        final AiConfiguration config = mockConfig(Map.of(
                "neodymium.ai.chat.provider", "openai",
                "neodymium.ai.chat.apiKey", "stale-gemini-role-key",
                "neodymium.ai.chat.openai.apiKey", "sk-role-openai-key"
        ));
        Mockito.when(config.getProvider("chat")).thenReturn("openai");
        Mockito.when(config.getTemperature("chat")).thenReturn(0.0);

        final AuraChatModelFactory factory = new AuraChatModelFactory(key -> null);
        Assertions.assertEquals("sk-role-openai-key", factory.resolveApiKey(config, "openai"));
    }

    @Test
    public void testMistralRestClientBuilderSupportsTimeoutAndNegativeTimeoutSafely()
    {
        Assertions.assertNotNull(AuraChatModelFactory.buildMistralRestClientBuilder(Duration.ofSeconds(45)).build());
        Assertions.assertNotNull(AuraChatModelFactory.buildMistralRestClientBuilder(Duration.ZERO).build());
        Assertions.assertNotNull(AuraChatModelFactory.buildMistralRestClientBuilder(null).build());
    }

    private static AiConfiguration mockConfig(final Map<String, String> properties)
    {
        final AiConfiguration config = Mockito.mock(AiConfiguration.class);
        Mockito.when(config.getProperty(Mockito.anyString(), Mockito.any()))
                .thenAnswer(invocation -> {
                    final String key = invocation.getArgument(0);
                    final String def = invocation.getArgument(1);
                    return properties.getOrDefault(key, def);
                });
        return config;
    }
}
