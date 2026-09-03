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
package org.neodymium.ai.pipeline.steps;

import org.neodymium.ai.audit.ExecutionAuditor;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline step executing post-run health, token usage, and quality auditing.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class PostExecutionAuditStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PostExecutionAuditStep.class);

    /** Internal execution auditor evaluating session health metrics */
    private final ExecutionAuditor auditor = new ExecutionAuditor();

    /**
     * Constructs a PostExecutionAuditStep.
     */
    public PostExecutionAuditStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        // 1. Retrieve the active session instance
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            LOGGER.debug("No active session found in context. Skipping post-execution audit.");
            return;
        }

        // 2. Perform post-session audit evaluating token metrics, RCA diagnoses, and verification warnings
        LOGGER.debug("Running post-execution health audit via ExecutionAuditor...");
        final boolean auditPassed = this.auditor.audit(session);

        // 3. Record audit status and findings in execution context transient data map
        context.getTransientData().put("auditPassed", auditPassed);
        context.getTransientData().put("auditFindings", this.auditor.getAuditFindings());

        LOGGER.info("Post-execution audit finished. Passed: {}, Findings count: {}", auditPassed, this.auditor.getAuditFindings().size());
    }

    /**
     * Gets the underlying auditor.
     *
     * @return the ExecutionAuditor instance
     */
    public ExecutionAuditor getAuditor()
    {
        return this.auditor;
    }
}
