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
package org.neodymium.ai.tool.guard;

import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;

/**
 * Interceptor interface for inspecting, validating, or modifying proposed tool calls
 * prior to browser dispatch or execution.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public interface ToolInterceptor
{
    /**
     * Intercepts a proposed tool call.
     *
     * @param call proposed tool call
     * @param context tool execution context
     * @param intent semantic intent of the enclosing step
     * @return interception verdict
     */
    InterceptionVerdict intercept(final ToolCall call, final ToolContext context, final SemanticIntent intent);
}
