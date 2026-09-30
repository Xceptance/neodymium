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
package org.neodymium.ai.executor.selenide.plugins;

import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.executor.selenide.plugins.FillFormAction.FormFieldEntry;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.common.browser.Browser;
import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit and browser integration tests for {@link FillFormAction}.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class FillFormActionTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public FillFormActionTest()
    {
    }

    @Test
    public void testExtractFieldsFromParameters()
    {
        final Action action = new Action("FILL_FORM", "form", "Fill form");
        action.getParameters().put("fields", List.of(
            Map.of("selector", "#user", "value", "admin"),
            Map.of("selector", "#pass", "value", "secret", "clearFirst", false)
        ));

        final List<FormFieldEntry> fields = FillFormAction.extractFields(action);
        assertEquals(2, fields.size());
        assertEquals("#user", fields.get(0).selector());
        assertEquals("admin", fields.get(0).value());
        assertTrue(fields.get(0).clearFirst());

        assertEquals("#pass", fields.get(1).selector());
        assertEquals("secret", fields.get(1).value());
        assertEquals(false, fields.get(1).clearFirst());
    }

    @Test
    public void testExtractFieldsFromToolCall()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        final ArrayNode arr = args.putArray("fields");
        arr.addObject().put("selector", "#email").put("value", "test@example.com");
        arr.addObject().put("target", "#zip").put("text", "12345");

        final ToolCall call = new ToolCall("call-1", "fill_form", args);
        final Action action = Action.fromToolCall(call);

        final List<FormFieldEntry> fields = FillFormAction.extractFields(action);
        assertEquals(2, fields.size());
        assertEquals("#email", fields.get(0).selector());
        assertEquals("test@example.com", fields.get(0).value());
        assertEquals("#zip", fields.get(1).selector());
        assertEquals("12345", fields.get(1).value());
    }

    @Test
    public void testExtractFieldsFromMapPayload()
    {
        final Action action = new Action("FILL_FORM", "form", "Fill form");
        action.getParameters().put("fields", Map.of("#fieldA", "valA", "#fieldB", "valB"));

        final List<FormFieldEntry> fields = FillFormAction.extractFields(action);
        assertEquals(2, fields.size());
    }

    @Test
    public void testExecuteFillFormReplay() throws Exception
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Fill Form Replay Test</title></head>
            <body>
                <form id="my-form">
                    <input id="inp-name" type="text" value="Old" />
                    <input id="inp-email" type="email" value="" />
                </form>
            </body>
            </html>
            """;
        try
        {
            Selenide.open("data:text/html;charset=utf-8," + html);

            final ExecutionContext context = new ExecutionContext(new SessionData(Map.of()));
            final SelenideTargetExecutor executor = new SelenideTargetExecutor();
            executor.setExecutionContext(context);
            context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
            final AiSession session = AiSession.mock(context.getSessionData(), null, null, executor);
            context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

            final Action action = new Action("FILL_FORM", "form", "Fill form");
            action.getParameters().put("fields", List.of(
                Map.of("selector", "#inp-name", "value", "Bob"),
                Map.of("selector", "#inp-email", "value", "bob@example.com")
            ));

            executor.execute(action);

            assertEquals("Bob", $("#inp-name").getValue());
            assertEquals("bob@example.com", $("#inp-email").getValue());
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }
}
