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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;

/**
 * General pre-invocation {@link ToolInterceptor} pipeline that evaluates an ordered list of
 * interceptors sequentially. Short-circuits on the first non-allowed verdict and forwards
 * deliberated {@link ToolCall} adjustments to subsequent interceptors in the chain.
 *
 * @author AI-generated: Claude Opus 4.6
 * @author Xceptance GmbH 2026
 */
public final class CompositeToolInterceptor implements ToolInterceptor
{
    private final List<ToolInterceptor> interceptors;

    /**
     * Constructs a composite interceptor from the supplied varargs chain.
     *
     * @param interceptors ordered interceptors to evaluate
     */
    public CompositeToolInterceptor(final ToolInterceptor... interceptors)
    {
        this(interceptors != null ? Arrays.asList(interceptors) : Collections.emptyList());
    }

    /**
     * Constructs a composite interceptor from the supplied list.
     *
     * @param interceptors ordered interceptors to evaluate
     */
    public CompositeToolInterceptor(final List<ToolInterceptor> interceptors)
    {
        final List<ToolInterceptor> valid = new ArrayList<>();
        if (interceptors != null)
        {
            for (final ToolInterceptor interceptor : interceptors)
            {
                if (interceptor != null)
                {
                    valid.add(interceptor);
                }
            }
        }
        this.interceptors = Collections.unmodifiableList(valid);
    }

    /**
     * Returns an unmodifiable view of the configured interceptor chain.
     *
     * @return ordered list of interceptors
     */
    public List<ToolInterceptor> getInterceptors()
    {
        return this.interceptors;
    }

    @Override
    public InterceptionVerdict intercept(final ToolCall call, final ToolContext context)
    {
        if (call == null || this.interceptors.isEmpty())
        {
            return InterceptionVerdict.allow("No interceptors configured");
        }

        ToolCall effectiveCall = call;
        InterceptionVerdict deliberatedVerdict = null;

        for (final ToolInterceptor interceptor : this.interceptors)
        {
            final InterceptionVerdict verdict = interceptor.intercept(effectiveCall, context);
            if (verdict == null)
            {
                continue;
            }
            if (!verdict.isAllowed())
            {
                return verdict;
            }
            if (verdict.decision() == InterceptionVerdict.Decision.DELIBERATED && verdict.adjustedCall() != null)
            {
                effectiveCall = verdict.adjustedCall();
                deliberatedVerdict = verdict;
            }
        }

        return deliberatedVerdict != null
                ? deliberatedVerdict
                : InterceptionVerdict.allow("All interceptors allowed tool call");
    }
}
