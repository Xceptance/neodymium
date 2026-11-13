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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolRegistry;

/**
 * Unit tests for {@link CompositeToolInterceptor}.
 *
 * @author AI-generated: Claude Opus 4.6
 * @author Xceptance GmbH 2026
 */
public class CompositeToolInterceptorTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testAllowsWhenAllInterceptorsAllow()
    {
        final List<String> order = new ArrayList<>();
        final ToolInterceptor first = (call, ctx) ->
        {
            order.add("first");
            return InterceptionVerdict.allow("first ok");
        };
        final ToolInterceptor second = (call, ctx) ->
        {
            order.add("second");
            return InterceptionVerdict.allow("second ok");
        };

        final CompositeToolInterceptor composite = new CompositeToolInterceptor(List.of(first, second));
        final ToolContext ctx = new SimpleToolContext(new ToolRegistry());
        final ToolCall call = new ToolCall("c1", "click", MAPPER.createObjectNode());

        final InterceptionVerdict verdict = composite.intercept(call, ctx);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(List.of("first", "second"), order);
    }

    @Test
    public void testShortCircuitsOnFirstBlockingVerdict()
    {
        final List<String> order = new ArrayList<>();
        final ToolInterceptor first = (call, ctx) ->
        {
            order.add("first");
            return InterceptionVerdict.retryWithFeedback(call.callId(), "blocked by first", "use UI");
        };
        final ToolInterceptor second = (call, ctx) ->
        {
            order.add("second");
            return InterceptionVerdict.allow("second ok");
        };

        final CompositeToolInterceptor composite = new CompositeToolInterceptor(first, second);
        final ToolContext ctx = new SimpleToolContext(new ToolRegistry());
        final ToolCall call = new ToolCall("c1", "navigate", MAPPER.createObjectNode());

        final InterceptionVerdict verdict = composite.intercept(call, ctx);

        Assertions.assertFalse(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.RETRY_WITH_FEEDBACK, verdict.decision());
        Assertions.assertEquals(List.of("first"), order);
    }

    @Test
    public void testPropagatesDeliberatedAdjustedCallToDownstreamInterceptor()
    {
        final ObjectNode revisedArgs = MAPPER.createObjectNode();
        revisedArgs.put("selector", "#better-id");
        final ToolCall revisedCall = new ToolCall("c1", "click", revisedArgs);

        final ToolInterceptor first = (call, ctx) -> InterceptionVerdict.deliberated(revisedCall, "upgraded selector");
        final List<String> seenSelectors = new ArrayList<>();
        final ToolInterceptor second = (call, ctx) ->
        {
            seenSelectors.add(call.arguments().path("selector").asText());
            return InterceptionVerdict.allow("downstream approved");
        };

        final CompositeToolInterceptor composite = new CompositeToolInterceptor(first, second);
        final ToolContext ctx = new SimpleToolContext(new ToolRegistry());
        final ObjectNode initialArgs = MAPPER.createObjectNode();
        initialArgs.put("selector", ".fragile");
        final ToolCall initialCall = new ToolCall("c1", "click", initialArgs);

        final InterceptionVerdict verdict = composite.intercept(initialCall, ctx);

        Assertions.assertTrue(verdict.isAllowed());
        Assertions.assertEquals(InterceptionVerdict.Decision.DELIBERATED, verdict.decision());
        Assertions.assertEquals("#better-id", verdict.getEffectiveCall(initialCall).arguments().path("selector").asText());
        Assertions.assertEquals(List.of("#better-id"), seenSelectors);
    }
}
