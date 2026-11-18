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
package org.neodymium.ai.event;

/**
 * Enumeration representing high-level event categories for event routing,
 * classification, and network filtering.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public enum EventCategory
{
    /** Core execution lifecycle steps and state changes */
    STRUCTURAL,

    /** Low-level LLM request and response interaction metrics */
    LLM,

    /** Diagnostic logs, warnings, soft errors, and RCA findings */
    DIAGNOSTIC,

    /** Aggregate session telemetry updates */
    TELEMETRY,

    /** Custom extension events published by plugins or hooks */
    CUSTOM
}
