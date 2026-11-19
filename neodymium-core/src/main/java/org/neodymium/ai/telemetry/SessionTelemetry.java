/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.telemetry;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable record representing session telemetry metrics, token consumption,
 * execution timing, and estimated API usage costs.
 *
 * @param totalSteps total number of playbook steps executed
 * @param healedSteps number of steps requiring self-healing or escalation
 * @param tokenUsageInput total input/prompt tokens consumed
 * @param tokenUsageOutput total output/completion tokens generated
 * @param tokenUsageCached total cached tokens re-used
 * @param totalDurationMs total session execution duration in milliseconds
 * @param estimatedCostUsd estimated API execution cost in USD
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record SessionTelemetry(
    @JsonProperty("totalSteps") int totalSteps,
    @JsonProperty("healedSteps") int healedSteps,
    @JsonProperty("tokenUsageInput") int tokenUsageInput,
    @JsonProperty("tokenUsageOutput") int tokenUsageOutput,
    @JsonProperty("tokenUsageCached") int tokenUsageCached,
    @JsonProperty("totalDurationMs") long totalDurationMs,
    @JsonProperty("estimatedCostUsd") double estimatedCostUsd
)
{
}
