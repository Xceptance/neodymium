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
package org.neodymium.ai.telemetry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener that accumulates real-time token usage, execution timing, self-healing
 * rates, and estimated cost metrics during session runs, exporting telemetry snapshots
 * to registered sinks (such as session-telemetry.json).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MetricsCollector implements ExecutionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);

    /** Standard default output path for session audit telemetry JSON */
    public static final Path DEFAULT_OUTPUT_PATH = Paths.get("target/ai-audit/session-telemetry.json");

    private final AtomicInteger totalSteps = new AtomicInteger(0);
    private final AtomicInteger healedSteps = new AtomicInteger(0);
    private final AtomicInteger tokenUsageInput = new AtomicInteger(0);
    private final AtomicInteger tokenUsageOutput = new AtomicInteger(0);
    private final AtomicInteger tokenUsageCached = new AtomicInteger(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);

    private double estimatedCostUsd = 0.0;

    /** Registered telemetry output sinks */
    private final List<TelemetrySink> sinks = new CopyOnWriteArrayList<>();

    /**
     * Constructs a default MetricsCollector registering a FileTelemetrySink with target/ai-audit/session-telemetry.json.
     */
    public MetricsCollector()
    {
        this(DEFAULT_OUTPUT_PATH);
    }

    /**
     * Constructs a MetricsCollector registering a FileTelemetrySink at the specified output path.
     *
     * @param outputPath target path for session-telemetry.json
     */
    public MetricsCollector(final Path outputPath)
    {
        if (outputPath != null)
        {
            this.sinks.add(new FileTelemetrySink(outputPath));
        }
    }

    /**
     * Registers a telemetry sink subscriber.
     *
     * @param sink the telemetry sink to add
     */
    public void addSink(final TelemetrySink sink)
    {
        if (sink != null && !this.sinks.contains(sink))
        {
            this.sinks.add(sink);
        }
    }

    /**
     * Removes a registered telemetry sink subscriber.
     *
     * @param sink the telemetry sink to remove
     */
    public void removeSink(final TelemetrySink sink)
    {
        if (sink != null)
        {
            this.sinks.remove(sink);
        }
    }

    /**
     * Consumes execution events to update real-time telemetry metrics.
     *
     * @param event the dispatched execution event
     */
    @Override
    public void onEvent(final ExecutionEvent event)
    {
        if (event == null)
        {
            return;
        }

        if (event instanceof StepStartedEvent)
        {
            this.totalSteps.incrementAndGet();
        }
        else if (event instanceof StepFinishedEvent stepEvent)
        {
            if (stepEvent.getStatus() == PlaybookStepStatus.HEALED)
            {
                this.healedSteps.incrementAndGet();
            }
        }
        else if (event instanceof LlmResponseReceivedEvent llmEvent)
        {
            if (llmEvent.getResponse() != null && llmEvent.getResponse().tokenUsage() != null)
            {
                final TokenUsage usage = llmEvent.getResponse().tokenUsage();
                this.tokenUsageInput.addAndGet(usage.inputTokenCount());
                this.tokenUsageOutput.addAndGet(usage.outputTokenCount());
                this.tokenUsageCached.addAndGet(usage.cachedTokenCount());

                final String modelName = llmEvent.getResponse().modelName();
                final double callCost = calculateCost(usage, modelName);
                synchronized (this)
                {
                    this.estimatedCostUsd += callCost;
                }
            }
        }
        else if (event instanceof SessionFinishedEvent sessionFinished)
        {
            this.totalDurationMs.set(sessionFinished.getDurationMs());
            notifySinks();
        }
    }

    /**
     * Notifies all registered telemetry sinks with the current telemetry snapshot.
     */
    public void notifySinks()
    {
        final SessionTelemetry snapshot = getTelemetrySnapshot();
        for (final TelemetrySink sink : this.sinks)
        {
            try
            {
                sink.consume(snapshot);
            }
            catch (final Exception e)
            {
                LOGGER.warn("Error notifying telemetry sink {}: {}", sink.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Compiles and returns a thread-safe immutable snapshot of the current session telemetry.
     *
     * @return current SessionTelemetry snapshot
     */
    public synchronized SessionTelemetry getTelemetrySnapshot()
    {
        return new SessionTelemetry(
            this.totalSteps.get(),
            this.healedSteps.get(),
            this.tokenUsageInput.get(),
            this.tokenUsageOutput.get(),
            this.tokenUsageCached.get(),
            this.totalDurationMs.get(),
            this.estimatedCostUsd
        );
    }

    /**
     * Immutable rate specification for token cost calculations per 1,000,000 tokens.
     *
     * @param inputRatePerMillion cost per 1M input tokens in USD
     * @param outputRatePerMillion cost per 1M output tokens in USD
     * @param cachedRatePerMillion cost per 1M cached prompt tokens in USD
     */
    public record ModelRate(double inputRatePerMillion, double outputRatePerMillion, double cachedRatePerMillion)
    {
    }

    /**
     * Resolves the token price rates for a given model.
     * Supported models: gemini-3.5-flash-lite, gemini-2.5-flash-lite, gemini-3.7-flash, gemini-3.5-flash, gemini-3.6-flash.
     * For any unknown model, a warning is logged and {@code null} is returned.
     *
     * @param modelName the model identifier
     * @return the resolved ModelRate, or {@code null} if the model is unknown
     */
    public static ModelRate getModelRate(final String modelName)
    {
        if (modelName == null || modelName.isBlank())
        {
            return null;
        }

        final String normalized = modelName.toLowerCase().replace('_', '-').trim();

        if (normalized.contains("3.5-flash-lite"))
        {
            return new ModelRate(0.30, 2.50, 0.075);
        }
        if (normalized.contains("2.5-flash-lite"))
        {
            return new ModelRate(0.10, 0.40, 0.025);
        }
        if (normalized.contains("3.7-flash"))
        {
            return new ModelRate(0.75, 3.75, 0.1875);
        }
        if (normalized.contains("3.6-flash"))
        {
            return new ModelRate(0.50, 3.00, 0.125);
        }
        if (normalized.contains("3.5-flash"))
        {
            return new ModelRate(0.50, 3.00, 0.125);
        }

        LOGGER.warn("⚠️ Unknown model '{}' encountered for cost estimation. Cost calculation skipped (set to $0.00).", modelName);
        return null;
    }

    /**
     * Helper method to compute estimated USD cost based on token counts and model pricing rules.
     *
     * @param usage the token usage
     * @param modelName the model identifier
     * @return estimated cost in USD, or 0.0 if usage is null or model is unknown
     */
    public static double calculateCost(final TokenUsage usage, final String modelName)
    {
        if (usage == null)
        {
            return 0.0;
        }

        final ModelRate rate = getModelRate(modelName);
        if (rate == null)
        {
            return 0.0;
        }

        final double inputCost = (usage.inputTokenCount() / 1_000_000.0) * rate.inputRatePerMillion();
        final double outputCost = (usage.outputTokenCount() / 1_000_000.0) * rate.outputRatePerMillion();
        final double cachedCost = (usage.cachedTokenCount() / 1_000_000.0) * rate.cachedRatePerMillion();

        return inputCost + outputCost + cachedCost;
    }
}
