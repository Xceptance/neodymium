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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying the state classifications and flags of {@link ExecutionMode}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionModeTest
{
    @Test
    public final void testLlmRecordingFlags()
    {
        final ExecutionMode mode = ExecutionMode.LLM_RECORDING;
        assertTrue(mode.isLive(), "LLM_RECORDING must be live");
        assertTrue(mode.isRecording(), "LLM_RECORDING must record executed actions");
        assertFalse(mode.isReplay(), "LLM_RECORDING must not be classified as a replay mode");
        assertFalse(mode.isLinterOnly(), "LLM_RECORDING is not linter-only");
        assertFalse(mode.supportsHealing(), "LLM_RECORDING does not use replay healing");
    }

    @Test
    public final void testForceRecordingFlags()
    {
        final ExecutionMode mode = ExecutionMode.FORCE_RECORDING;
        assertTrue(mode.isLive(), "FORCE_RECORDING must be live");
        assertTrue(mode.isRecording(), "FORCE_RECORDING must record executed actions");
        assertFalse(mode.isReplay(), "FORCE_RECORDING must not be classified as a replay mode");
        assertFalse(mode.isLinterOnly(), "FORCE_RECORDING is not linter-only");
        assertFalse(mode.supportsHealing(), "FORCE_RECORDING does not use replay healing");
    }

    @Test
    public final void testLlmOnlyFlags()
    {
        final ExecutionMode mode = ExecutionMode.LLM_ONLY;
        assertTrue(mode.isLive(), "LLM_ONLY must be live");
        assertFalse(mode.isRecording(), "LLM_ONLY must not record actions to disk");
        assertFalse(mode.isReplay(), "LLM_ONLY must not be replay");
        assertFalse(mode.isLinterOnly(), "LLM_ONLY is not linter-only");
        assertFalse(mode.supportsHealing(), "LLM_ONLY does not use replay healing");
    }

    @Test
    public final void testReplayWithHealingFlags()
    {
        final ExecutionMode mode = ExecutionMode.REPLAY_WITH_HEALING;
        assertFalse(mode.isLive(), "REPLAY_WITH_HEALING is not live execution");
        assertFalse(mode.isRecording(), "REPLAY_WITH_HEALING is not recording mode");
        assertTrue(mode.isReplay(), "REPLAY_WITH_HEALING must be replay mode");
        assertFalse(mode.isLinterOnly(), "REPLAY_WITH_HEALING is not linter-only");
        assertTrue(mode.supportsHealing(), "REPLAY_WITH_HEALING supports healing");
    }

    @Test
    public final void testReplayStrictFlags()
    {
        final ExecutionMode mode = ExecutionMode.REPLAY_STRICT;
        assertFalse(mode.isLive(), "REPLAY_STRICT is not live execution");
        assertFalse(mode.isRecording(), "REPLAY_STRICT is not recording mode");
        assertTrue(mode.isReplay(), "REPLAY_STRICT must be replay mode");
        assertFalse(mode.isLinterOnly(), "REPLAY_STRICT is not linter-only");
        assertFalse(mode.supportsHealing(), "REPLAY_STRICT does not support healing");
    }

    @Test
    public final void testLinterOnlyFlags()
    {
        final ExecutionMode mode = ExecutionMode.LINTER_ONLY;
        assertTrue(mode.isLive(), "LINTER_ONLY executes upfront prompt linting live");
        assertFalse(mode.isRecording(), "LINTER_ONLY does not record browser actions");
        assertFalse(mode.isReplay(), "LINTER_ONLY is not replay");
        assertTrue(mode.isLinterOnly(), "LINTER_ONLY must return true for isLinterOnly()");
        assertFalse(mode.supportsHealing(), "LINTER_ONLY does not support healing");
    }
}
