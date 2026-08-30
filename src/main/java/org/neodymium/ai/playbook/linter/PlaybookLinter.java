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
package org.neodymium.ai.playbook.linter;

import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ReasoningEffort;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.CallLlmStep;
import org.neodymium.ai.prompt.PlaybookLinterPrompt;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service executing upfront pre-flight static analysis on playbook scenario steps.
 * Runs in a single batch call during session initialization, providing non-blocking advisory findings.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookLinter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybookLinter.class);

    private final AiSession session;

    /**
     * Constructs a PlaybookLinter bound to the active AI session.
     *
     * @param session the active AI session
     */
    public PlaybookLinter(final AiSession session)
    {
        this.session = session;
    }

    /**
     * Runs upfront static analysis on the provided scenario steps.
     *
     * @param steps the list of playbook steps
     * @param description optional high-level scenario description
     * @return list of advisory findings, or empty list if disabled or no issues found
     */
    public List<PlaybookLinterFinding> lint(final List<PlaybookStep> steps, final String description)
    {
        if (steps == null || steps.isEmpty())
        {
            return Collections.emptyList();
        }

        final ExecutionMode mode = this.session != null ? this.session.getExecutionMode() : null;
        if (mode != null && !mode.isLive())
        {
            LOGGER.debug("Playbook pre-flight linter is bypassed in replay mode ({}).", mode);
            return Collections.emptyList();
        }

        final AiConfiguration config = AiConfiguration.getInstance();
        boolean enabled = config.isLinterEnabled();

        if (this.session != null && this.session.data() != null)
        {
            final Object dynamicVal = this.session.data().get("neodymium.ai.linter.enabled");
            if (dynamicVal != null)
            {
                enabled = Boolean.parseBoolean(String.valueOf(dynamicVal).trim());
            }
        }

        if (!enabled)
        {
            LOGGER.debug("Playbook pre-flight linter is disabled (neodymium.ai.linter.enabled=false).");
            return Collections.emptyList();
        }

        final ExecutionContext context = this.session != null ? this.session.getExecutionContext() : ExecutionContext.getActiveContext();

        try
        {
            final LlmProvider provider = this.session != null && this.session.getLlmRegistry() != null
                ? this.session.getLlmRegistry().getProvider(LlmCapability.LINTER)
                : null;

            if (provider == null)
            {
                LOGGER.warn("⚠️ No LLM provider registered for capability {}", LlmCapability.LINTER);
                return Collections.emptyList();
            }

            final PlaybookLinterPrompt prompt = new PlaybookLinterPrompt(description, steps, context);
            final String sysMsg = prompt.compileSystemMessage(context);
            final String userMsg = prompt.compileUserMessage(context);

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Playbook Linter System Prompt:\n{}", sysMsg);
                LOGGER.trace("Playbook Linter User Prompt:\n{}", userMsg);
            }

            final double temperature = config.getTemperature("linter");
            final int timeoutSeconds = config.getTimeoutSeconds("linter");

            final LlmRequest request = new LlmRequest(
                sysMsg,
                userMsg,
                Collections.emptyList(),
                ResponseSchema.LINTER,
                temperature,
                timeoutSeconds,
                ReasoningEffort.LOW
            );

            if (this.session != null && this.session.getEventBus() != null)
            {
                this.session.getEventBus().dispatch(new LlmRequestSentEvent(request, "LINTER"));
            }

            LOGGER.debug("🔍 Executing upfront playbook pre-flight linting on {} step(s)...", steps.size());
            final long startTime = System.currentTimeMillis();
            final LlmResponse response = provider.chat(request);
            final long durationMs = System.currentTimeMillis() - startTime;

            if (LOGGER.isDebugEnabled() && response != null && response.content() != null)
            {
                LOGGER.debug("   🔍 [Playbook Linter Response]:\n{}", CallLlmStep.formatJsonForLogging(response.content()));
            }

            // Track tokens and call count
            if (context != null && response != null)
            {
                final TokenUsage usage = response.tokenUsage();
                if (usage != null)
                {
                    final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_LINTER_TOKEN_USAGE);
                    final TokenUsage updated = existing != null
                        ? new TokenUsage(
                            existing.inputTokenCount() + usage.inputTokenCount(),
                            existing.outputTokenCount() + usage.outputTokenCount(),
                            existing.totalTokenCount() + usage.totalTokenCount(),
                            existing.cachedTokenCount() + usage.cachedTokenCount())
                        : usage;
                    context.getTransientData().put(ExecutionContext.KEY_LINTER_TOKEN_USAGE, updated);
                }

                final Integer existingCalls = (Integer) context.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT);
                context.getTransientData().put(ExecutionContext.KEY_LINTER_CALL_COUNT, (existingCalls != null ? existingCalls : 0) + 1);
            }

            // Emit LLM response event
            if (this.session != null && this.session.getEventBus() != null && response != null)
            {
                this.session.getEventBus().dispatch(new LlmResponseReceivedEvent(
                    request,
                    response,
                    durationMs,
                    "LINTER"
                ));
            }

            final List<PlaybookLinterFinding> findings = prompt.parseResponse(response != null ? response.content() : "", context);
            if (findings.isEmpty())
            {
                LOGGER.info("🔍 Pre-flight playbook linter completed in {} ms: 0 advisory finding(s) detected.", durationMs);
            }
            else
            {
                LOGGER.info("🔍 Pre-flight playbook linter completed in {} ms: {} advisory finding(s) detected.", durationMs, findings.size());
                for (final PlaybookLinterFinding finding : findings)
                {
                    LOGGER.info("   ⚠️  [Step #{}{}] [{} / {}] {}",
                        finding.stepIndex(),
                        finding.lineNumber() > 0 ? ", line " + finding.lineNumber() : "",
                        finding.category(),
                        finding.severity(),
                        finding.message());
                    if (finding.suggestedRewrite() != null && !finding.suggestedRewrite().isBlank())
                    {
                        LOGGER.info("      💡 Suggested Rewrite: {}", finding.suggestedRewrite().replace("\n", "\n         "));
                    }
                }
            }

            if (context != null)
            {
                context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS, findings);
            }

            return findings;
        }
        catch (final Throwable t)
        {
            LOGGER.warn("⚠️ Pre-flight playbook linter encountered an error and was skipped gracefully: {}", t.getMessage(), t);
            return Collections.emptyList();
        }
    }
}
