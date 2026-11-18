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
package org.neodymium.ai.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-execution auditor that evaluates token usage, performance anomalies, failure diagnostics,
 * and produces structured audit reports for AI test runs.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionAuditor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionAuditor.class);

    /** Recorded findings and quality warnings discovered during session auditing */
    private final List<String> auditFindings = new ArrayList<>();

    /** Overall audit status flag (true if session meets all quality standards) */
    private boolean passed = true;

    /**
     * Constructs a default ExecutionAuditor instance.
     */
    public ExecutionAuditor()
    {
    }

    /**
     * Performs an audit on the completed or failed AI session.
     *
     * @param session the target AiSession
     * @return true if session meets all health & quality thresholds, false if warnings/anomalies detected
     */
    public boolean audit(final AiSession session)
    {
        // 1. Guard against null session parameter
        if (session == null)
        {
            throw new IllegalArgumentException("session cannot be null");
        }

        // 2. Reset findings state for new audit execution
        this.auditFindings.clear();
        this.passed = true;

        // 3. Retrieve execution context from target session
        final ExecutionContext context = session.getExecutionContext();
        if (context == null)
        {
            this.auditFindings.add("Execution context was null");
            this.passed = false;
            return false;
        }

        // 4. Audit token consumption against configured soft threshold (100,000 total tokens)
        final TokenUsage stdToken = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
        if (stdToken != null && stdToken.totalTokenCount() > 100_000)
        {
            this.auditFindings.add("High token consumption detected: " + stdToken.totalTokenCount() + " tokens used.");
        }

        // 5. Check if any Visual RCA diagnoses were recorded during test run
        final String rcaSummary = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY);
        if (rcaSummary != null && !rcaSummary.isBlank())
        {
            this.auditFindings.add("Visual RCA finding recorded: " + rcaSummary);
            this.passed = false;
        }

        // 6. Check for semantic outcome verification warnings recorded during step execution
        @SuppressWarnings("unchecked")
        final List<String> warnings = (List<String>) context.getTransientData().get("verificationWarnings");
        if (warnings != null && !warnings.isEmpty())
        {
            this.auditFindings.add("Verification warnings present (" + warnings.size() + " warning(s)).");
            this.passed = false;
        }

        LOGGER.info("Session audit completed. Status: {}, Findings: {}", this.passed ? "PASS" : "WARN", this.auditFindings);
        return this.passed;
    }

    /**
     * Gets unmodifiable list of audit findings.
     *
     * @return list of audit finding descriptions
     */
    public List<String> getAuditFindings()
    {
        return Collections.unmodifiableList(this.auditFindings);
    }

    /**
     * Gets overall audit result.
     *
     * @return true if passed, false otherwise
     */
    public boolean isPassed()
    {
        return this.passed;
    }
}
