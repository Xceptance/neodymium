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
package org.neodymium.ai.executor.probe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Normalized summary of an on-screen element captured during live locator probing.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProbeElementSummary
{
    private final int index;
    private final String tagName;
    private final String text;
    private final Map<String, String> attributes;
    private final boolean visible;
    private final boolean enabled;
    private final boolean selected;
    private final ProbeBoundingRect rect;
    private final String outerHtmlSnippet;

    /**
     * Constructs an immutable element probe summary.
     *
     * @param index element match index (0-based)
     * @param tagName HTML tag name
     * @param text visible inner text
     * @param attributes map of standard W3C attributes
     * @param visible whether element is visible
     * @param enabled whether element is interactable/enabled
     * @param selected whether element is selected/checked
     * @param rect bounding rectangle
     * @param outerHtmlSnippet truncated outer HTML snippet
     */
    @JsonCreator
    public ProbeElementSummary(
            @JsonProperty("index") final int index,
            @JsonProperty("tagName") final String tagName,
            @JsonProperty("text") final String text,
            @JsonProperty("attributes") final Map<String, String> attributes,
            @JsonProperty("visible") final boolean visible,
            @JsonProperty("enabled") final boolean enabled,
            @JsonProperty("selected") final boolean selected,
            @JsonProperty("rect") final ProbeBoundingRect rect,
            @JsonProperty("outerHtmlSnippet") final String outerHtmlSnippet)
    {
        this.index = index;
        this.tagName = tagName != null ? tagName : "";
        this.text = text != null ? text.trim() : "";
        this.attributes = attributes != null ? Collections.unmodifiableMap(new LinkedHashMap<>(attributes)) : Collections.emptyMap();
        this.visible = visible;
        this.enabled = enabled;
        this.selected = selected;
        this.rect = rect;
        this.outerHtmlSnippet = outerHtmlSnippet != null ? outerHtmlSnippet.trim() : "";
    }

    /**
     * Convenience constructor for probe summaries with default empty attributes and snippets.
     *
     * @param index element index
     * @param tagName HTML tag name
     * @param text visible inner text
     * @param visible whether element is visible
     * @param enabled whether element is enabled
     * @param rect bounding rectangle
     */
    public ProbeElementSummary(
            final int index,
            final String tagName,
            final String text,
            final boolean visible,
            final boolean enabled,
            final ProbeBoundingRect rect)
    {
        this(index, tagName, text, Collections.emptyMap(), visible, enabled, false, rect, "");
    }

    public int getIndex()
    {
        return this.index;
    }

    public String getTagName()
    {
        return this.tagName;
    }

    public String getText()
    {
        return this.text;
    }

    public Map<String, String> getAttributes()
    {
        return this.attributes;
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isSelected()
    {
        return this.selected;
    }

    public ProbeBoundingRect getRect()
    {
        return this.rect;
    }

    public String getOuterHtmlSnippet()
    {
        return this.outerHtmlSnippet;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof final ProbeElementSummary other))
        {
            return false;
        }
        return this.index == other.index
                && this.visible == other.visible
                && this.enabled == other.enabled
                && this.selected == other.selected
                && Objects.equals(this.tagName, other.tagName)
                && Objects.equals(this.text, other.text)
                && Objects.equals(this.attributes, other.attributes)
                && Objects.equals(this.rect, other.rect)
                && Objects.equals(this.outerHtmlSnippet, other.outerHtmlSnippet);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.index, this.tagName, this.text, this.attributes, this.visible, this.enabled, this.selected, this.rect, this.outerHtmlSnippet);
    }

    @Override
    public String toString()
    {
        return String.format("<%s> \"%s\" (visible=%b, enabled=%b, rect=%s)",
                this.tagName, this.text, this.visible, this.enabled, this.rect != null ? this.rect.toString() : "none");
    }
}
