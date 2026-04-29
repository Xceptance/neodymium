package com.xceptance.neodymium.ai.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks cumulative token usage statistics across all LLM calls.
 * Thread-safe via atomic counters.
 */
public class TokenStats {
    private static final Logger LOG = LoggerFactory.getLogger(TokenStats.class);

    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();
    private final AtomicInteger callCount = new AtomicInteger();

    /**
     * Records a single LLM call's token usage.
     *
     * @param input  number of input (prompt) tokens
     * @param output number of output (completion) tokens
     */
    public void record(final long input, final long output) {
        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
        callCount.incrementAndGet();

        LOG.debug("   📊 Tokens: {} in → {} out  (call #{})",
                input, output, callCount.get());
    }

    /**
     * Logs a summary of all cumulative token usage.
     */
    public void logSummary() {
        final long totalIn = inputTokens.get();
        final long totalOut = outputTokens.get();
        final long total = totalIn + totalOut;

        LOG.debug("======== 📊 Token Usage Summary ========");
        LOG.debug("   LLM calls:      {}", callCount.get());
        LOG.debug("   Input tokens:   {}", totalIn);
        LOG.debug("   Output tokens:  {}", totalOut);
        LOG.debug("   Total tokens:   {}", total);
        LOG.debug("========================================");
    }

    public long getInputTokens() {
        return inputTokens.get();
    }

    public long getOutputTokens() {
        return outputTokens.get();
    }

    public long getTotalTokens() {
        return inputTokens.get() + outputTokens.get();
    }

    public int getCallCount() {
        return callCount.get();
    }

    /**
     * Resets all counters.
     */
    public void reset() {
        inputTokens.set(0);
        outputTokens.set(0);
        callCount.set(0);
    }
}
