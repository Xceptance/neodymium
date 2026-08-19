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
package org.neodymium.ai.debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Diagnostic and debugging helper for inspecting AI test session state, token metrics,
 * execution history, and visual RCA diagnostic results.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class SessionDebugger
{
    /** The target AI session being inspected */
    private final AiSession session;

    /**
     * Constructs a SessionDebugger for the target AI session.
     *
     * @param session the AI session to debug
     */
    public SessionDebugger(final AiSession session)
    {
        // Fail-fast guard against null session parameter
        if (session == null)
        {
            throw new IllegalArgumentException("session cannot be null");
        }
        this.session = session;
    }

    /**
     * Gets the associated AI session.
     *
     * @return the AiSession
     */
    public AiSession getSession()
    {
        return this.session;
    }

    /**
     * Retrieves the last Visual RCA diagnosis summary from the session context if present.
     *
     * @return the RCA diagnosis string, or empty string if none available
     */
    public String getLastRcaSummary()
    {
        // 1. Fetch execution context from active session
        final ExecutionContext context = this.session.getExecutionContext();
        if (context == null)
        {
            return "";
        }
        // 2. Extract visual RCA summary key from transient map
        final String summary = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY);
        return summary != null ? summary : "";
    }

    /**
     * Retrieves the last semantic divergence diff summary from the session context if present.
     *
     * @return the semantic diff summary string, or empty string if none available
     */
    public String getLastSemanticDiffSummary()
    {
        // 1. Fetch execution context from active session
        final ExecutionContext context = this.session.getExecutionContext();
        if (context == null)
        {
            return "";
        }
        // 2. Extract semantic diff summary key from transient map
        final String diff = (String) context.getTransientData().get(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY);
        return diff != null ? diff : "";
    }

    /**
     * Retrieves token usage breakdown from session context.
     *
     * @return map of token usage categories
     */
    public Map<String, TokenUsage> getTokenUsageBreakdown()
    {
        // 1. Retrieve context safely
        final ExecutionContext context = this.session.getExecutionContext();
        if (context == null)
        {
            return Collections.emptyMap();
        }

        final Map<String, TokenUsage> result = new HashMap<>();

        // 2. Extract standard step execution token metrics
        final TokenUsage std = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
        if (std != null)
        {
            result.put("standard", std);
        }

        // 3. Extract semantic verification token metrics
        final TokenUsage ver = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);
        if (ver != null)
        {
            result.put("verification", ver);
        }

        // 4. Extract PESAP token metrics
        final TokenUsage pesap = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_PESAP_TOKEN_USAGE);
        if (pesap != null)
        {
            result.put("pesap", pesap);
        }

        // 5. Extract Quality Judge token metrics
        final TokenUsage judge = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_JUDGE_TOKEN_USAGE);
        if (judge != null)
        {
            result.put("judge", judge);
        }

        // 6. Extract Visual RCA token metrics
        final TokenUsage rca = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_RCA_TOKEN_USAGE);
        if (rca != null)
        {
            result.put("rca", rca);
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Dumps complete session state snapshot as a formatted debugging string.
     *
     * @return debug report string
     */
    public String dumpSessionReport()
    {
        // 1. Format human-readable diagnostic dump string
        final StringBuilder sb = new StringBuilder();
        sb.append("=== AI Session Debug Report ===\n");
        sb.append("Session ID: ").append(this.session.hashCode()).append("\n");
        sb.append("Last RCA Diagnosis: ").append(getLastRcaSummary()).append("\n");
        sb.append("Last Semantic Diff: ").append(getLastSemanticDiffSummary()).append("\n");
        sb.append("Token Usage Categories: ").append(getTokenUsageBreakdown().keySet()).append("\n");
        sb.append("===============================\n");
        return sb.toString();
    }
}
