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
package org.neodymium.ai.config;

/**
 * Defines the execution behavior of the AI pipeline when processing playbook steps.
 * This determines whether the system uses the LLM to dynamically generate actions,
 * replays recorded actions, and how it handles failures during execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public enum ExecutionMode
{
    /**
     * Live execution using the LLM to determine actions.
     * Does not record the executed steps to a playbook recording JSON file.
     */
    LLM_ONLY,

    /**
     * Live execution using the LLM to determine actions,
     * and automatically records the executed steps to a playbook recording JSON file.
     */
    LLM_RECORDING,

    /**
     * Enforces live execution using the LLM to determine actions and records them,
     * completely ignoring any pre-existing playbook recordings on disk.
     */
    FORCE_RECORDING,

    /**
     * Replays pre-recorded actions.
     * Failures trigger a healing path where the SUT state is compared against the recorded baseline
     * to generate a semantic diff, which is then used by the LLM to heal the step.
     */
    REPLAY_WITH_HEALING,

    /**
     * Replays pre-recorded actions strictly.
     * Failures are conclusive and terminate execution immediately (no LLM fallback).
     */
    REPLAY_STRICT,

    /**
     * Static playbook pre-flight linting only.
     * Executes upfront pre-flight linter analysis and generates quality reports, skipping browser/target step execution.
     */
    LINTER_ONLY,

    /**
     * Smart execution mode.
     * <p>
     * Dynamically determines whether to execute in {@link #REPLAY_WITH_HEALING} or {@link #LLM_RECORDING} mode:
     * <ul>
     *   <li>If a recorded companion JSON file exists on disk, it executes as {@link #REPLAY_WITH_HEALING}
     *       (replaying recorded actions and using the LLM only to heal failures, persisting any updates).</li>
     *   <li>If no recorded companion JSON file exists on disk, it executes as {@link #LLM_RECORDING}
     *       (generating actions live via the LLM and saving the new companion recording to disk upon success).</li>
     * </ul>
     */
    AUTO;

    /**
     * Returns true if this mode performs live LLM action generation or live LLM linting.
     */
    public boolean isLive()
    {
        return this == LLM_ONLY || this == LLM_RECORDING || this == FORCE_RECORDING || this == LINTER_ONLY;
    }

    /**
     * Returns true if this mode performs static pre-flight linting only.
     */
    public boolean isLinterOnly()
    {
        return this == LINTER_ONLY;
    }

    /**
     * Returns true if this mode automatically selects between replay with healing and live recording
     * based on whether a companion recording file exists on disk.
     */
    public boolean isAuto()
    {
        return this == AUTO;
    }

    /**
     * Returns true if this mode automatically records executed actions.
     */
    public boolean isRecording()
    {
        return this == LLM_RECORDING || this == FORCE_RECORDING;
    }

    /**
     * Returns true if a successful session in this mode may create or update the playbook recording file.
     * <p>
     * That is the case for the recording modes (new recording), {@link #REPLAY_WITH_HEALING} (healed step updates),
     * and {@link #AUTO} (which resolves dynamically to one of those modes).
     * It is deliberately not the case for {@link #LLM_ONLY} (documented as not recording), {@link #LINTER_ONLY}
     * (pre-flight linting only), or {@link #REPLAY_STRICT} (strictly read-only replay that never touches recordings on disk).
     */
    public boolean persistsRecording()
    {
        return isRecording() || this == REPLAY_WITH_HEALING || this == AUTO;
    }

    /**
     * Returns true if this mode supports replaying recorded actions.
     */
    public boolean isReplay()
    {
        return this == REPLAY_WITH_HEALING || this == REPLAY_STRICT;
    }

    /**
     * Returns true if this mode supports healing replay failures via LLM.
     */
    public boolean supportsHealing()
    {
        return this == REPLAY_WITH_HEALING || this == AUTO;
    }
}
