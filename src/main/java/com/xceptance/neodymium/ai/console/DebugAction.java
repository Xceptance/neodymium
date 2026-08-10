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
package com.xceptance.neodymium.ai.console;

/**
 * Represents a user-triggered action in the Interactive Console, sent from
 * the browser UI to the Java test runner via HTTP POST.
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author Xceptance GmbH 2026
 */
public enum DebugAction
{
    /** Approve the current step and continue execution. */
    RUN,

    /** Skip the current step without executing it. */
    SKIP,

    /** Rewind execution back to a given step index. */
    REWIND,

    /** Insert a new instruction before the current step. */
    ADD,

    /** Edit an existing planned instruction at a given index. */
    EDIT,

    /** Append a new instruction to the end of the planned list. */
    APPEND,

    /** Reorder a planned step from one index to another. */
    REORDER,

    /** Save changes to the YAML file and exit interactive mode. */
    SAVE_EXIT,

    /** Trigger AI healing analysis on a failed step. */
    HEAL,

    /** Accept current step failure state and finish test execution. */
    FINISH,

    /** Dump the current page context and DOM to a debug file. */
    DUMP,

    /** Persist UI display settings (e.g. panel layout preferences). */
    SETTINGS;
}
