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
package com.xceptance.aura.chat.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Builds and caches Spring AI {@link ChatModel} instances dynamically from Neodymium's
 * {@link AiConfiguration} for the {@code "chat"} role, supporting Gemini, OpenAI, and Mistral
 * with provider-isolated API key and model resolution.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
@Component
public class AuraChatModelFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChatModelFactory.class);
    private static final String CHAT_ROLE = "chat";
    private static final String PROVIDER_GEMINI = "gemini";
    private static final String PROVIDER_OPENAI = "openai";
    private static final String PROVIDER_MISTRAL = "mistral";
    private static final int MAX_MODEL_CACHE_SIZE = 16;

    private final Function<String, String> envLookup;
    private final Map<ModelCacheKey, CachedChatModel> modelCache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true)
            {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<ModelCacheKey, CachedChatModel> eldest)
                {
                    return size() > MAX_MODEL_CACHE_SIZE;
                }
            });

    /**
     * Constructs the factory using {@link System#getenv(String)} for environment API-key fallback.
     */
    @Autowired
    public AuraChatModelFactory()
    {
        this(System::getenv);
    }

    AuraChatModelFactory(final Function<String, String> envLookup)
    {
        this.envLookup = Objects.requireNonNull(envLookup, "envLookup must not be null");
    }

    /**
     * Creates or retrieves a cached Spring AI {@link ChatModel} using the active {@link AiConfiguration}.
     *
     * @return configured {@link ChatModel}
     * @throws IllegalStateException if the provider or API key is missing or unsupported
     */
    public ChatModel createChatModel()
    {
        return createChatModel(AiConfiguration.getInstance());
    }

    /**
     * Creates or retrieves a cached Spring AI {@link ChatModel} from the supplied {@link AiConfiguration}.
     *
     * @param config Neodymium AI configuration
     * @return configured {@link ChatModel}
     * @throws IllegalStateException if the provider or API key is missing or unsupported
     */
    public ChatModel createChatModel(final AiConfiguration config)
    {
        if (config == null)
        {
            throw new IllegalStateException("AiConfiguration must not be null.");
        }

        final String provider = resolveProvider(config);
        final String apiKey = resolveApiKey(config, provider);
        final String modelName = resolveModelName(config, provider);
        final double temperature = config.getTemperature(CHAT_ROLE);
        final String baseUrl = resolveBaseUrl(config, provider);
        final int configuredTimeout = config.getTimeoutSeconds(CHAT_ROLE);
        final Duration timeout = Duration.ofSeconds(configuredTimeout > 0 ? configuredTimeout : 180);

        final ModelCacheKey cacheKey = new ModelCacheKey(provider, apiKey, modelName, temperature, baseUrl, timeout);
        synchronized (this.modelCache)
        {
            final CachedChatModel cached = this.modelCache.get(cacheKey);
            if (cached != null)
            {
                return cached.model();
            }
            final CachedChatModel created = switch (cacheKey.provider())
            {
                case PROVIDER_GEMINI -> createGeminiModel(
                        cacheKey.baseUrl(), cacheKey.apiKey(), cacheKey.modelName(), cacheKey.temperature(), cacheKey.timeout());
                case PROVIDER_OPENAI -> createOpenAiModel(
                        cacheKey.baseUrl(), cacheKey.apiKey(), cacheKey.modelName(), cacheKey.temperature(), cacheKey.timeout());
                case PROVIDER_MISTRAL -> createMistralModel(
                        cacheKey.baseUrl(), cacheKey.apiKey(), cacheKey.modelName(), cacheKey.temperature(), cacheKey.timeout());
                default -> throw new IllegalStateException(
                        "Unsupported chat LLM provider '" + cacheKey.provider()
                                + "'. Supported providers for Spring AI ChatClient are: gemini, openai, mistral.");
            };
            this.modelCache.put(cacheKey, created);
            return created.model();
        }
    }

    /**
     * Releases cached HTTP/SDK clients when the Spring context shuts down without interrupting active turns during runtime LRU eviction.
     */
    @PreDestroy
    public void closeCachedClients()
    {
        final List<CachedChatModel> snapshot;
        synchronized (this.modelCache)
        {
            snapshot = new ArrayList<>(this.modelCache.values());
            this.modelCache.clear();
        }
        for (final CachedChatModel entry : snapshot)
        {
            entry.closeQuietly();
        }
    }

    int getCacheSize()
    {
        return this.modelCache.size();
    }

    String resolveProvider(final AiConfiguration config)
    {
        final String rawProvider = config.getProvider(CHAT_ROLE);
        return canonicalizeProvider(rawProvider);
    }

    String resolveModelName(final AiConfiguration config, final String canonicalProvider)
    {
        final String chatModel = trimToNull(config.getProperty("neodymium.ai.chat.model", null));
        if (chatModel != null)
        {
            return chatModel;
        }

        final String providerSpecificModel = trimToNull(config.getProperty("neodymium.ai." + canonicalProvider + ".model", null));
        if (providerSpecificModel != null)
        {
            return providerSpecificModel;
        }

        final String globalProvider = resolveGlobalProvider(config);
        if (canonicalProvider.equals(globalProvider))
        {
            final String globalModel = trimToNull(config.getProperty("neodymium.ai.model", null));
            if (globalModel != null)
            {
                return globalModel;
            }
        }

        return PROVIDER_GEMINI.equals(canonicalProvider) ? trimToNull(config.getModel(CHAT_ROLE)) : null;
    }

    String resolveApiKey(final AiConfiguration config, final String canonicalProvider)
    {
        final String roleProviderKey = trimToNull(
                config.getProperty("neodymium.ai.chat." + canonicalProvider + ".apiKey", null));
        if (roleProviderKey != null)
        {
            return roleProviderKey;
        }

        final String chatKey = trimToNull(config.getProperty("neodymium.ai.chat.apiKey", null));
        if (chatKey != null)
        {
            return chatKey;
        }

        final String providerPropKey = trimToNull(config.getProperty("neodymium.ai." + canonicalProvider + ".apiKey", null));
        if (providerPropKey != null)
        {
            return providerPropKey;
        }

        final String globalProvider = resolveGlobalProvider(config);
        final String envVarName = envVarForProvider(canonicalProvider);
        final String envKey = envVarName != null ? trimToNull(this.envLookup.apply(envVarName)) : null;

        if (!canonicalProvider.equals(globalProvider))
        {
            if (envKey != null)
            {
                return envKey;
            }
        }
        else
        {
            final String globalKey = trimToNull(config.aiApiKey());
            if (globalKey != null)
            {
                return globalKey;
            }
            if (envKey != null)
            {
                return envKey;
            }
        }

        throw new IllegalStateException(
                "API Key is missing or invalid for provider '" + canonicalProvider
                        + "'. Please configure 'neodymium.ai.chat.apiKey', 'neodymium.ai." + canonicalProvider
                        + ".apiKey', 'neodymium.ai.apiKey', or environment variable "
                        + (envVarName != null ? envVarName : "GEMINI_API_KEY") + ".");
    }

    private static String resolveBaseUrl(final AiConfiguration config, final String canonicalProvider)
    {
        final String chatBaseUrl = trimToNull(config.getProperty("neodymium.ai.chat.baseUrl", null));
        if (chatBaseUrl != null)
        {
            return chatBaseUrl;
        }
        final String providerBaseUrl = trimToNull(config.getProperty("neodymium.ai." + canonicalProvider + ".baseUrl", null));
        if (providerBaseUrl != null)
        {
            return providerBaseUrl;
        }
        return trimToNull(config.getProperty("neodymium.ai.baseUrl", null));
    }

    private static String resolveGlobalProvider(final AiConfiguration config)
    {
        final String rawGlobal = trimToNull(config.getProperty("neodymium.ai.provider", null));
        return rawGlobal != null ? canonicalizeProvider(rawGlobal) : PROVIDER_GEMINI;
    }

    private static String canonicalizeProvider(final String rawProvider)
    {
        final String trimmed = trimToNull(rawProvider);
        if (trimmed == null)
        {
            return PROVIDER_GEMINI;
        }
        return switch (trimmed.toLowerCase())
        {
            case "gemini", "google", "google-genai" -> PROVIDER_GEMINI;
            case "openai" -> PROVIDER_OPENAI;
            case "mistral" -> PROVIDER_MISTRAL;
            default -> trimmed.toLowerCase();
        };
    }

    private static String envVarForProvider(final String canonicalProvider)
    {
        return switch (canonicalProvider)
        {
            case PROVIDER_GEMINI -> "GEMINI_API_KEY";
            case PROVIDER_OPENAI -> "OPENAI_API_KEY";
            case PROVIDER_MISTRAL -> "MISTRAL_API_KEY";
            default -> null;
        };
    }

    private static String trimToNull(final String value)
    {
        if (value == null)
        {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static HttpOptions buildGeminiHttpOptions(final Duration timeout, final String baseUrl)
    {
        final HttpOptions.Builder builder = HttpOptions.builder();
        if (timeout != null && !timeout.isZero() && !timeout.isNegative())
        {
            final long millisLong = timeout.toMillis();
            final int millis = millisLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millisLong;
            builder.timeout(millis);
        }
        if (baseUrl != null && !baseUrl.isBlank())
        {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }

    private static CachedChatModel createGeminiModel(final String baseUrl, final String apiKey, final String modelName,
                                                     final double temperature, final Duration timeout)
    {
        final Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(buildGeminiHttpOptions(timeout, baseUrl))
                .build();
        final GoogleGenAiChatOptions.Builder optionsBuilder = GoogleGenAiChatOptions.builder()
                .temperature(temperature);
        if (modelName != null && !modelName.isBlank())
        {
            optionsBuilder.model(modelName.trim());
        }
        final ChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .options(optionsBuilder.build())
                .build();
        return CachedChatModel.of(model, client);
    }

    private static CachedChatModel createOpenAiModel(final String baseUrl, final String apiKey,
                                                     final String modelName, final double temperature,
                                                     final Duration timeout)
    {
        final OpenAIClient openAiClient = OpenAiSetup.setupSyncClient(
                baseUrl, apiKey, null, null, null, null, false, false,
                null, timeout, 0, null, null, ObservationRegistry.NOOP, null, List.of());
        final OpenAIClientAsync openAiClientAsync = OpenAiSetup.setupAsyncClient(
                baseUrl, apiKey, null, null, null, null, false, false,
                null, timeout, 0, null, null, ObservationRegistry.NOOP, null, List.of());
        final OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .temperature(temperature);
        if (modelName != null && !modelName.isBlank())
        {
            optionsBuilder.model(modelName.trim());
        }
        final ChatModel model = OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .openAiClientAsync(openAiClientAsync)
                .options(optionsBuilder.build())
                .build();
        return CachedChatModel.of(model, openAiClient, openAiClientAsync);
    }

    static RestClient.Builder buildMistralRestClientBuilder(final Duration timeout)
    {
        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        final boolean hasPositiveTimeout = timeout != null && !timeout.isZero() && !timeout.isNegative();
        if (hasPositiveTimeout)
        {
            httpClientBuilder.connectTimeout(timeout);
        }
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClientBuilder.build());
        if (hasPositiveTimeout)
        {
            requestFactory.setReadTimeout(timeout);
        }
        return RestClient.builder().requestFactory(requestFactory);
    }

    private static CachedChatModel createMistralModel(final String baseUrl, final String apiKey,
                                                      final String modelName, final double temperature,
                                                      final Duration timeout)
    {
        final MistralAiApi.Builder apiBuilder = MistralAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(buildMistralRestClientBuilder(timeout));
        if (baseUrl != null && !baseUrl.isBlank())
        {
            apiBuilder.baseUrl(baseUrl.trim());
        }
        final MistralAiChatOptions.Builder optionsBuilder = MistralAiChatOptions.builder()
                .temperature(temperature);
        if (modelName != null && !modelName.isBlank())
        {
            optionsBuilder.model(modelName.trim());
        }
        final ChatModel model = MistralAiChatModel.builder()
                .mistralAiApi(apiBuilder.build())
                .options(optionsBuilder.build())
                .build();
        return CachedChatModel.of(model);
    }

    private record CachedChatModel(ChatModel model, List<AutoCloseable> closeables)
    {
        private static CachedChatModel of(final ChatModel model, final Object... resources)
        {
            final List<AutoCloseable> handles = new ArrayList<>();
            if (resources != null)
            {
                for (final Object resource : resources)
                {
                    if (resource instanceof AutoCloseable closeable)
                    {
                        handles.add(closeable);
                    }
                }
            }
            return new CachedChatModel(model, List.copyOf(handles));
        }

        private void closeQuietly()
        {
            for (final AutoCloseable closeable : this.closeables)
            {
                try
                {
                    closeable.close();
                }
                catch (final Exception e)
                {
                    LOGGER.debug("Ignoring error while closing cached chat client: {}", e.getMessage());
                }
            }
        }
    }

    private record ModelCacheKey(String provider, String apiKey, String modelName,
                                 double temperature, String baseUrl, Duration timeout)
    {
        @Override
        public String toString()
        {
            return "ModelCacheKey[provider=" + provider + ", apiKey=***, modelName=" + modelName
                    + ", temperature=" + temperature + ", baseUrl=" + baseUrl + ", timeout=" + timeout + "]";
        }
    }
}
