/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.util.layer;

/**
 * Backend-agnostic element condition used by {@link ActionExecutor} and all AI action plugins
 * instead of the Selenide-specific {@code Condition} API.
 * <p>
 * Each condition carries optional string parameters (e.g. expected text, attribute name/value,
 * regex pattern) to support rich assertion semantics without coupling to any driver framework.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class ElementCondition
{
    /**
     * Enumeration of all supported condition types.
     */
    public enum Type
    {
        /** Element exists in the DOM (may be hidden). */
        EXIST,
        /** Element is visible (rendered and not hidden). */
        VISIBLE,
        /** Element is hidden or absent. */
        HIDDEN,
        /** Element currently holds keyboard focus. */
        FOCUSED,
        /** Element's visible text contains the given substring. */
        TEXT_CONTAINS,
        /** Element's visible text exactly matches the given string. */
        TEXT_EXACT,
        /** Input element's value attribute equals the given string. */
        VALUE_IS,
        /** Any DOM attribute of the element contains the given substring. */
        ANY_ATTR_CONTAINS,
        /** A specific DOM attribute of the element equals the given value. */
        ATTR_IS,
        /** Element's text, textContent, or any attribute matches the given regex pattern. */
        MATCHES_REGEX
    }

    private final Type type;

    /** Primary string parameter (expected text, value, attribute name, or regex). */
    private final String param1;

    /** Secondary string parameter (attribute value when {@code type == ATTR_IS}). */
    private final String param2;

    private ElementCondition(final Type type, final String param1, final String param2)
    {
        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    // ─── Factory methods ─────────────────────────────────────────────────────

    /** @return condition that asserts the element exists in the DOM. */
    public static ElementCondition exist()
    {
        return new ElementCondition(Type.EXIST, null, null);
    }

    /** @return condition that asserts the element is visible on the page. */
    public static ElementCondition visible()
    {
        return new ElementCondition(Type.VISIBLE, null, null);
    }

    /** @return condition that asserts the element is hidden or absent. */
    public static ElementCondition hidden()
    {
        return new ElementCondition(Type.HIDDEN, null, null);
    }

    /** @return condition that asserts the element is currently focused. */
    public static ElementCondition focused()
    {
        return new ElementCondition(Type.FOCUSED, null, null);
    }

    /**
     * @param text the substring to look for in the element's visible text
     * @return condition that asserts the element's text contains {@code text}
     */
    public static ElementCondition textContains(final String text)
    {
        return new ElementCondition(Type.TEXT_CONTAINS, text, null);
    }

    /**
     * @param text the exact visible text to match
     * @return condition that asserts the element's text exactly equals {@code text}
     */
    public static ElementCondition textExact(final String text)
    {
        return new ElementCondition(Type.TEXT_EXACT, text, null);
    }

    /**
     * @param value the expected input value
     * @return condition that asserts the element's {@code value} attribute equals {@code value}
     */
    public static ElementCondition valueIs(final String value)
    {
        return new ElementCondition(Type.VALUE_IS, value, null);
    }

    /**
     * @param substring the substring to find in any DOM attribute
     * @return condition that asserts at least one DOM attribute contains {@code substring}
     */
    public static ElementCondition anyAttrContains(final String substring)
    {
        return new ElementCondition(Type.ANY_ATTR_CONTAINS, substring, null);
    }

    /**
     * @param name  the attribute name
     * @param value the expected attribute value
     * @return condition that asserts the named attribute equals {@code value}
     */
    public static ElementCondition attrIs(final String name, final String value)
    {
        return new ElementCondition(Type.ATTR_IS, name, value);
    }

    /**
     * @param regex the regular expression to match against text / attributes
     * @return condition that asserts a match against {@code regex}
     */
    public static ElementCondition matchesRegex(final String regex)
    {
        return new ElementCondition(Type.MATCHES_REGEX, regex, null);
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    /** @return the condition type */
    public Type getType() { return type; }

    /** @return the primary parameter (text, regex, or attribute name), may be {@code null} */
    public String getParam1() { return param1; }

    /** @return the secondary parameter (attribute value for {@link Type#ATTR_IS}), may be {@code null} */
    public String getParam2() { return param2; }

    @Override
    public String toString()
    {
        return param2 != null
               ? type + "(" + param1 + ", " + param2 + ")"
               : param1 != null ? type + "(" + param1 + ")" : type.name();
    }
}
