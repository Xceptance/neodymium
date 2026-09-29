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
package org.neodymium.ai.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable representation of an interactive DOM element's structural, semantic,
 * and text features, used for sub-millisecond local similarity matching during replay.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DomFeatureVector implements Serializable
{
    private static final long serialVersionUID = 1L;

    @JsonProperty("tag")
    private final String tag;

    @JsonProperty("text")
    private final String text;

    @JsonProperty("classes")
    private final Set<String> classes;

    @JsonProperty("attributes")
    private final Map<String, String> attributes;

    @JsonProperty("role")
    private final String role;

    @JsonProperty("accessibleName")
    private final String accessibleName;

    @JsonProperty("parentTag")
    private final String parentTag;

    @JsonProperty("siblingIndex")
    private final int siblingIndex;

    @JsonProperty("x")
    private final int x;

    @JsonProperty("y")
    private final int y;

    @JsonProperty("width")
    private final int width;

    @JsonProperty("height")
    private final int height;

    @JsonProperty("visualHash")
    private final String visualHash;

    @JsonProperty("tileSsim")
    private final String tileSsim;

    /**
     * Constructs a new DomFeatureVector with all fields including spatial bounding box geometry,
     * perceptual visual dHash, and tile SSIM luminance matrix.
     *
     * @param tag the HTML tag name
     * @param text the visible text content
     * @param classes the CSS class set
     * @param attributes the attribute key-value map
     * @param role the explicit or computed ARIA role
     * @param accessibleName the computed accessible name
     * @param parentTag the immediate parent element tag
     * @param siblingIndex the 0-based sibling index
     * @param x the bounding box X coordinate
     * @param y the bounding box Y coordinate
     * @param width the bounding box width
     * @param height the bounding box height
     * @param visualHash perceptual visual dHash
     * @param tileSsim Base64 tile SSIM matrix
     */
    @JsonCreator
    public DomFeatureVector(
        @JsonProperty("tag") final String tag,
        @JsonProperty("text") final String text,
        @JsonProperty("classes") final Set<String> classes,
        @JsonProperty("attributes") final Map<String, String> attributes,
        @JsonProperty("role") final String role,
        @JsonProperty("accessibleName") final String accessibleName,
        @JsonProperty("parentTag") final String parentTag,
        @JsonProperty("siblingIndex") final int siblingIndex,
        @JsonProperty("x") final Integer x,
        @JsonProperty("y") final Integer y,
        @JsonProperty("width") final Integer width,
        @JsonProperty("height") final Integer height,
        @JsonProperty("visualHash") final String visualHash,
        @JsonProperty("tileSsim") final String tileSsim)
    {
        this.tag = tag != null ? tag.trim().toLowerCase() : "";
        this.text = text != null ? text.trim() : "";
        this.classes = classes != null ? Collections.unmodifiableSet(new LinkedHashSet<>(classes)) : Collections.emptySet();
        this.attributes = attributes != null ? Collections.unmodifiableMap(new LinkedHashMap<>(attributes)) : Collections.emptyMap();
        this.role = role != null ? role.trim().toLowerCase() : "";
        this.accessibleName = accessibleName != null ? accessibleName.trim() : "";
        this.parentTag = parentTag != null ? parentTag.trim().toLowerCase() : "";
        this.siblingIndex = siblingIndex >= 0 ? siblingIndex : 0;
        this.x = x != null ? x : 0;
        this.y = y != null ? y : 0;
        this.width = width != null ? width : 0;
        this.height = height != null ? height : 0;
        this.visualHash = visualHash != null ? visualHash.trim() : "";
        this.tileSsim = tileSsim != null ? tileSsim.trim() : "";
    }

    /**
     * Constructs a new DomFeatureVector with spatial bounding box geometry.
     *
     * @param tag the HTML tag name
     * @param text the visible text content
     * @param classes the CSS class set
     * @param attributes the attribute key-value map
     * @param role the explicit or computed ARIA role
     * @param accessibleName the computed accessible name
     * @param parentTag the immediate parent element tag
     * @param siblingIndex the 0-based sibling index
     * @param x the bounding box X coordinate
     * @param y the bounding box Y coordinate
     * @param width the bounding box width
     * @param height the bounding box height
     */
    public DomFeatureVector(
        final String tag,
        final String text,
        final Set<String> classes,
        final Map<String, String> attributes,
        final String role,
        final String accessibleName,
        final String parentTag,
        final int siblingIndex,
        final Integer x,
        final Integer y,
        final Integer width,
        final Integer height)
    {
        this(tag, text, classes, attributes, role, accessibleName, parentTag, siblingIndex, x, y, width, height, "", "");
    }

    /**
     * Constructs a new DomFeatureVector without explicit bounding box geometry.
     *
     * @param tag the HTML tag name
     * @param text the visible text content
     * @param classes the CSS class set
     * @param attributes the attribute key-value map
     * @param role the explicit or computed ARIA role
     * @param accessibleName the computed accessible name
     * @param parentTag the immediate parent element tag
     * @param siblingIndex the 0-based sibling index
     */
    public DomFeatureVector(
        final String tag,
        final String text,
        final Set<String> classes,
        final Map<String, String> attributes,
        final String role,
        final String accessibleName,
        final String parentTag,
        final int siblingIndex)
    {
        this(tag, text, classes, attributes, role, accessibleName, parentTag, siblingIndex, 0, 0, 0, 0);
    }

    /**
     * Returns the normalized tag name.
     *
     * @return the tag name
     */
    public String getTag()
    {
        return tag;
    }

    /**
     * Returns the text content.
     *
     * @return the text
     */
    public String getText()
    {
        return text;
    }

    /**
     * Returns the unmodifiable set of CSS classes.
     *
     * @return the classes
     */
    public Set<String> getClasses()
    {
        return classes;
    }

    /**
     * Returns the unmodifiable map of element attributes.
     *
     * @return the attributes map
     */
    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    /**
     * Returns the ARIA role.
     *
     * @return the role
     */
    public String getRole()
    {
        return role;
    }

    /**
     * Returns the computed accessible name.
     *
     * @return the accessible name
     */
    public String getAccessibleName()
    {
        return accessibleName;
    }

    /**
     * Returns the parent element tag.
     *
     * @return the parent tag
     */
    public String getParentTag()
    {
        return parentTag;
    }

    /**
     * Returns the sibling index.
     *
     * @return the sibling index
     */
    public int getSiblingIndex()
    {
        return siblingIndex;
    }

    /**
     * Returns the bounding box X coordinate.
     *
     * @return the X coordinate
     */
    public int getX()
    {
        return x;
    }

    /**
     * Returns the bounding box Y coordinate.
     *
     * @return the Y coordinate
     */
    public int getY()
    {
        return y;
    }

    /**
     * Returns the bounding box width.
     *
     * @return the width
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Returns the bounding box height.
     *
     * @return the height
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Returns the perceptual visual dHash if present.
     *
     * @return the visual dHash string
     */
    public String getVisualHash()
    {
        return this.visualHash;
    }

    /**
     * Returns the Base64 tile SSIM luminance matrix if present.
     *
     * @return the tile SSIM matrix string
     */
    public String getTileSsim()
    {
        return this.tileSsim;
    }

    /**
     * Returns true if this feature vector contains a non-empty visual dHash.
     *
     * @return true if visual hash is available
     */
    public boolean hasVisualHash()
    {
        return this.visualHash != null && !this.visualHash.isBlank();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        final DomFeatureVector other = (DomFeatureVector) obj;
        return siblingIndex == other.siblingIndex
            && x == other.x
            && y == other.y
            && width == other.width
            && height == other.height
            && Objects.equals(tag, other.tag)
            && Objects.equals(text, other.text)
            && Objects.equals(classes, other.classes)
            && Objects.equals(attributes, other.attributes)
            && Objects.equals(role, other.role)
            && Objects.equals(accessibleName, other.accessibleName)
            && Objects.equals(parentTag, other.parentTag);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tag, text, classes, attributes, role, accessibleName, parentTag, siblingIndex, x, y, width, height);
    }

    /**
     * Returns a concise, single-line human-readable summary of this feature vector.
     *
     * @return the short summary representation
     */
    public String toSummaryString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("<").append(tag.isEmpty() ? "*" : tag).append(">");
        if (!text.isEmpty())
        {
            final String cleanText = text.length() > 30 ? text.substring(0, 27) + "..." : text;
            sb.append(" \"").append(cleanText).append("\"");
        }
        else if (!accessibleName.isEmpty())
        {
            final String cleanAcc = accessibleName.length() > 30 ? accessibleName.substring(0, 27) + "..." : accessibleName;
            sb.append(" [").append(cleanAcc).append("]");
        }
        if (!parentTag.isEmpty())
        {
            sb.append(" parent=<").append(parentTag).append(">#").append(siblingIndex);
        }
        sb.append(" (").append(x).append(", ").append(y).append(", ").append(width).append("x").append(height).append(")");
        return sb.toString();
    }

    /**
     * Formats this feature vector into structured multi-line segments to avoid terminal line wrapping.
     *
     * @param firstLinePrefix    the prefix string for the first line
     * @param continuationPrefix the prefix string for subsequent continuation lines
     * @return list of formatted lines
     */
    public java.util.List<String> toFormattedLines(final String firstLinePrefix, final String continuationPrefix)
    {
        final java.util.List<String> lines = new java.util.ArrayList<>();

        // Line 1: Tag, Role, Text
        final StringBuilder line1 = new StringBuilder();
        line1.append(firstLinePrefix != null ? firstLinePrefix : "");
        line1.append("tag=<").append(tag).append(">");
        if (!role.isEmpty())
        {
            line1.append(", role='").append(role).append("'");
        }
        if (!text.isEmpty())
        {
            line1.append(", text='").append(text).append("'");
        }
        lines.add(line1.toString());

        final String cont = continuationPrefix != null ? continuationPrefix : "";

        // Line 2: Accessible name (if distinct) and CSS classes (if present)
        if ((!accessibleName.isEmpty() && !accessibleName.equals(text)) || !classes.isEmpty())
        {
            final StringBuilder line2 = new StringBuilder(cont);
            boolean hasPrev = false;
            if (!accessibleName.isEmpty() && !accessibleName.equals(text))
            {
                line2.append("accName='").append(accessibleName).append("'");
                hasPrev = true;
            }
            if (!classes.isEmpty())
            {
                if (hasPrev)
                {
                    line2.append(", ");
                }
                line2.append("classes=").append(classes);
            }
            lines.add(line2.toString());
        }

        // Line 3: Attributes (if present)
        if (!attributes.isEmpty())
        {
            lines.add(cont + "attrs=" + attributes);
        }

        // Line 4: Parent hierarchy and spatial bounding box
        final StringBuilder line4 = new StringBuilder(cont);
        if (!parentTag.isEmpty())
        {
            line4.append("parent=<").append(parentTag).append(">#").append(siblingIndex).append(", ");
        }
        line4.append("bounds=(").append(x).append(", ").append(y).append(", ").append(width).append("x").append(height).append(")");
        lines.add(line4.toString());

        return lines;
    }

    /**
     * Returns a compact, single-line human-readable summary of this feature vector for structured logging.
     *
     * @return the detail string representation
     */
    public String toDetailString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("tag=<").append(tag).append(">");
        if (!text.isEmpty())
        {
            sb.append(", text='").append(text).append("'");
        }
        if (!role.isEmpty())
        {
            sb.append(", role='").append(role).append("'");
        }
        if (!accessibleName.isEmpty() && !accessibleName.equals(text))
        {
            sb.append(", accName='").append(accessibleName).append("'");
        }
        if (!classes.isEmpty())
        {
            sb.append(", classes=").append(classes);
        }
        if (!attributes.isEmpty())
        {
            sb.append(", attrs=").append(attributes);
        }
        if (!parentTag.isEmpty())
        {
            sb.append(", parent=<").append(parentTag).append(">#").append(siblingIndex);
        }
        sb.append(", bounds=(").append(x).append(", ").append(y).append(", ").append(width).append("x").append(height).append(")");
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "DomFeatureVector{"
            + "tag='" + tag + '\''
            + ", text='" + text + '\''
            + ", classes=" + classes
            + ", attributes=" + attributes
            + ", role='" + role + '\''
            + ", accessibleName='" + accessibleName + '\''
            + ", parentTag='" + parentTag + '\''
            + ", siblingIndex=" + siblingIndex
            + ", x=" + x
            + ", y=" + y
            + ", width=" + width
            + ", height=" + height
            + '}';
    }
}
