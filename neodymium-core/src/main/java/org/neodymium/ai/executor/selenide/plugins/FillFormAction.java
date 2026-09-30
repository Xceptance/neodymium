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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Concrete action plugin executing FILL_FORM browser commands.
 * Sequentially populates multiple form fields (inputs, textareas, selects)
 * in a single atomic action during playback.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class FillFormAction implements BrowserActionPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FillFormAction.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a FillFormAction.
     */
    public FillFormAction()
    {
    }

    /**
     * Represents a single field to be filled in a form.
     *
     * @param selector the target element selector
     * @param value the text or option value to fill
     * @param clearFirst whether to clear before typing
     */
    public record FormFieldEntry(String selector, String value, boolean clearFirst)
    {
    }

    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final List<FormFieldEntry> fields = extractFields(action);
        if (fields.isEmpty())
        {
            LOGGER.warn("FILL_FORM action contains no fields to populate: {}", action);
            return;
        }

        for (final FormFieldEntry field : fields)
        {
            if (field.selector() == null || field.selector().isBlank())
            {
                continue;
            }

            final SelenideElement el = SelenideElementFinder.findElement(field.selector());
            SelenideElementFinder.scrollIntoViewIfNeeded(el);

            fillElement(el, field.value(), field.clearFirst());
        }
    }

    public static void fillElement(final SelenideElement el, final String text, final boolean clearFirst)
    {
        final String val = text != null ? text : "";
        final String tagName = el.getTagName() != null ? el.getTagName().toLowerCase() : "";

        if ("select".equals(tagName))
        {
            el.shouldBe(Condition.visible);
            if (!SelectAction.selectOptionFuzzy(el, val))
            {
                try
                {
                    el.selectOptionByValue(val);
                }
                catch (final Throwable e)
                {
                    el.selectOption(val);
                }
            }
            return;
        }

        final boolean isContentEditable = Boolean.TRUE.equals(Selenide.executeJavaScript(
            "return !!(arguments[0] && (arguments[0].isContentEditable === true || arguments[0].getAttribute('contenteditable') === 'true' || arguments[0].hasAttribute('contenteditable')));",
            el));

        if (isContentEditable)
        {
            el.shouldBe(Condition.visible);
            Selenide.executeJavaScript(
                "var el = arguments[0];"
                + "var clear = arguments[1];"
                + "el.focus();"
                + "if (clear) { el.innerHTML = ''; }"
                + "var range = document.createRange();"
                + "range.selectNodeContents(el);"
                + "range.collapse(false);"
                + "var sel = window.getSelection();"
                + "sel.removeAllRanges();"
                + "sel.addRange(range);",
                el, clearFirst);
            if (!val.isEmpty())
            {
                el.sendKeys(val);
            }
            Selenide.executeJavaScript(
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                el);
        }
        else
        {
            el.shouldBe(Condition.visible).shouldBe(Condition.editable);
            if (clearFirst)
            {
                try
                {
                    el.val(val);
                }
                catch (final Exception e)
                {
                    el.clear();
                    el.sendKeys(val);
                }
            }
            else
            {
                el.sendKeys(val);
            }
        }
    }

    public static List<FormFieldEntry> extractFields(final Action action)
    {
        final List<FormFieldEntry> result = new ArrayList<>();

        // 1. From action parameters
        final Object paramFields = action.getParameters().get("fields");
        if (paramFields != null)
        {
            parseFieldsObject(paramFields, result);
            if (!result.isEmpty())
            {
                return result;
            }
        }

        // 2. From toolCall arguments
        if (action.getToolCall() != null && action.getToolCall().arguments() != null)
        {
            final JsonNode args = action.getToolCall().arguments();
            if (args.hasNonNull("fields"))
            {
                parseFieldsJsonNode(args.get("fields"), result);
                if (!result.isEmpty())
                {
                    return result;
                }
            }
        }

        // 3. From action value
        final Object val = action.getValue();
        if (val != null)
        {
            parseFieldsObject(val, result);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static void parseFieldsObject(final Object raw, final List<FormFieldEntry> out)
    {
        if (raw instanceof final JsonNode jsonNode)
        {
            parseFieldsJsonNode(jsonNode, out);
        }
        else if (raw instanceof final List<?> list)
        {
            for (final Object item : list)
            {
                if (item instanceof final Map<?, ?> map)
                {
                    final Object selObj = map.get("selector") != null ? map.get("selector") : map.get("target");
                    final String sel = selObj != null ? String.valueOf(selObj) : "";
                    final Object valObj = map.get("value") != null ? map.get("value") : map.get("text");
                    final String value = valObj != null ? String.valueOf(valObj) : "";
                    final boolean clear = !map.containsKey("clearFirst") || Boolean.parseBoolean(String.valueOf(map.get("clearFirst")));
                    if (!sel.isBlank())
                    {
                        out.add(new FormFieldEntry(sel, value, clear));
                    }
                }
                else if (item != null)
                {
                    try
                    {
                        final JsonNode node = MAPPER.valueToTree(item);
                        parseFieldsJsonNode(node, out);
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
            }
        }
        else if (raw instanceof final Map<?, ?> map)
        {
            for (final Map.Entry<?, ?> entry : map.entrySet())
            {
                final String sel = String.valueOf(entry.getKey());
                final String value = String.valueOf(entry.getValue());
                if (!sel.isBlank())
                {
                    out.add(new FormFieldEntry(sel, value, true));
                }
            }
        }
        else if (raw instanceof final String str)
        {
            final String trimmed = str.trim();
            if (trimmed.startsWith("[") || trimmed.startsWith("{"))
            {
                try
                {
                    final JsonNode parsed = MAPPER.readTree(trimmed);
                    parseFieldsJsonNode(parsed, out);
                }
                catch (final Exception ignored)
                {
                }
            }
        }
    }

    public static void parseFieldsJsonNode(final JsonNode fieldsNode, final List<FormFieldEntry> out)
    {
        if (fieldsNode.isArray())
        {
            for (final JsonNode item : fieldsNode)
            {
                final String selector = item.hasNonNull("selector")
                        ? item.path("selector").asText()
                        : (item.hasNonNull("target") ? item.path("target").asText() : "");
                final String value = item.hasNonNull("value")
                        ? item.path("value").asText()
                        : (item.hasNonNull("text") ? item.path("text").asText() : "");
                final boolean clearFirst = !item.has("clearFirst") || item.path("clearFirst").asBoolean(true);

                if (!selector.isBlank())
                {
                    out.add(new FormFieldEntry(selector, value, clearFirst));
                }
            }
        }
        else if (fieldsNode.isObject())
        {
            final Iterator<Map.Entry<String, JsonNode>> it = fieldsNode.fields();
            while (it.hasNext())
            {
                final Map.Entry<String, JsonNode> entry = it.next();
                final String selector = entry.getKey();
                final String value = entry.getValue().asText();
                if (!selector.isBlank())
                {
                    out.add(new FormFieldEntry(selector, value, true));
                }
            }
        }
    }
}
