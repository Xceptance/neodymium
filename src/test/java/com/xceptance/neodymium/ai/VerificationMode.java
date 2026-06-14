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
package com.xceptance.neodymium.ai;

/**
 * Execution modes for AI test verification phases.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public enum VerificationMode
{
    /**
     * Executes live against the LLM to generate and save the baseline playbook.
     * Checks for agreement/drift against the playbook saved before the current run.
     */
    LIVE_LLM,

    /**
     * Replays the test case offline utilizing the saved playbook.
     * Asserts that zero LLM calls are made.
     */
    OFFLINE_REPLAY,

    /**
     * Replays the test case offline with the playbook, but permits LLM fallbacks/healing.
     */
    REPLAY,

    /**
     * Executes live against the LLM with the automated HUD overlay active.
     */
    HUD_LLM,

    /**
     * Replays the test case offline with the automated HUD overlay active.
     * Asserts that the HUD does not disrupt execution and zero LLM calls are made.
     */
    HUD_OFFLINE_REPLAY,

    /**
     * Replays the test case offline with the HUD active, but permits LLM fallbacks/healing.
     */
    HUD_REPLAY
}
