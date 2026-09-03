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
package org.neodymium.ai.client;

/**
 * Declares provider-neutral reasoning/thinking effort tiers for LLM requests.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public enum ReasoningEffort
{
    /**
     * Reasoning/thinking tokens disabled for maximum speed.
     */
    OFF,

    /**
     * Fast lightweight thinking (~512 tokens) for simple classification, step splitting, and intent routing.
     */
    LOW,

    /**
     * Balanced reasoning (~1024 tokens) for candidate quality scoring and visual RCA.
     */
    MEDIUM,

    /**
     * Deep reasoning (~2048+ tokens) for comprehensive DOM analysis, action synthesis, and assertion verification.
     */
    HIGH
}
