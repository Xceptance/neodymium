/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
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
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.neodymium.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests validating YAML tag parsing, hierarchical merging, and CLI override discovery in {@link TagUtils}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class TagUtilsTest
{
    @Test
    public void testParseTagsWithScalarsAndLists()
    {
        final Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("tag3", "value");
        raw.put("tag4", List.of("value2", "value3"));
        raw.put("numTag", 123);
        raw.put("boolTag", true);
        raw.put("flagTag", List.of());
        raw.put("emptyTag", "   ");
        raw.put("nullTag", null);

        final Map<String, List<String>> parsed = TagUtils.parseTags(raw);

        Assertions.assertEquals(5, parsed.size());
        Assertions.assertEquals(List.of("value"), parsed.get("tag3"));
        Assertions.assertEquals(List.of("value2", "value3"), parsed.get("tag4"));
        Assertions.assertEquals(List.of("123"), parsed.get("numTag"));
        Assertions.assertEquals(List.of("true"), parsed.get("boolTag"));
        Assertions.assertEquals(List.of(), parsed.get("flagTag"));
        Assertions.assertNull(parsed.get("emptyTag"));
        Assertions.assertNull(parsed.get("nullTag"));
    }

    @Test
    public void testParseTagsHandlesNonMap()
    {
        Assertions.assertTrue(TagUtils.parseTags(null).isEmpty());
        Assertions.assertTrue(TagUtils.parseTags("not a map").isEmpty());
        Assertions.assertTrue(TagUtils.parseTags(List.of("item")).isEmpty());
    }

    @Test
    public void testGetSystemPropertyOverrides()
    {
        final Properties props = new Properties();
        props.setProperty("neodymium.tag.tag1", "value231");
        props.setProperty("neodymium.tag.tag4", "overrideA, overrideB");
        props.setProperty("other.property", "ignored");

        final Map<String, List<String>> overrides = TagUtils.getSystemPropertyOverrides(props);

        Assertions.assertEquals(2, overrides.size());
        Assertions.assertEquals(List.of("value231"), overrides.get("tag1"));
        Assertions.assertEquals(List.of("overrideA", "overrideB"), overrides.get("tag4"));
    }

    @Test
    public void testMergeTagsPrecedenceHierarchy()
    {
        // 1. Root tags
        final Map<String, List<String>> rootTags = new LinkedHashMap<>();
        rootTags.put("tag3", List.of("rootVal3"));
        rootTags.put("tag4", List.of("val2", "val3"));
        rootTags.put("tagInherited", List.of("rootInherited"));

        // 2. Dataset tags
        final Map<String, List<String>> datasetTags = new LinkedHashMap<>();
        datasetTags.put("tag1", List.of("dsVal1"));
        datasetTags.put("tag4", List.of("dsVal4A", "dsVal4B")); // Overrides root tag4

        // 3. System property overrides
        final Map<String, List<String>> systemOverrides = new LinkedHashMap<>();
        systemOverrides.put("tag1", List.of("value231")); // Overrides dataset tag1
        systemOverrides.put("cliNewTag", List.of("cliOnly"));

        final Map<String, List<String>> merged = TagUtils.mergeTags(rootTags, datasetTags, systemOverrides);

        Assertions.assertEquals(5, merged.size());
        // System overrides win
        Assertions.assertEquals(List.of("value231"), merged.get("tag1"));
        Assertions.assertEquals(List.of("cliOnly"), merged.get("cliNewTag"));
        // Dataset overrides root
        Assertions.assertEquals(List.of("dsVal4A", "dsVal4B"), merged.get("tag4"));
        // Root tags preserved when not overridden
        Assertions.assertEquals(List.of("rootVal3"), merged.get("tag3"));
        Assertions.assertEquals(List.of("rootInherited"), merged.get("tagInherited"));
    }

    @Test
    public void testMergeTagsCaseInsensitiveOverride()
    {
        final Map<String, List<String>> rootTags = Map.of("Tag1", List.of("initial"));
        final Map<String, List<String>> datasetTags = Map.of("tag1", List.of("datasetVal"));
        final Map<String, List<String>> cliOverrides = Map.of("TAG1", List.of("cliVal"));

        final Map<String, List<String>> merged = TagUtils.mergeTags(rootTags, datasetTags, cliOverrides);

        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(List.of("cliVal"), merged.get("TAG1"));
    }

    @Test
    public void testFindTagValueAndValues()
    {
        final Map<String, List<String>> tags = Map.of(
            "Release", List.of("2026.10", "current"),
            "single", List.of("val")
        );

        Assertions.assertEquals("2026.10", TagUtils.findTagValue(tags, "release"));
        Assertions.assertEquals("2026.10", TagUtils.findTagValue(tags, "Release"));
        Assertions.assertEquals("val", TagUtils.findTagValue(tags, "SINGLE"));
        Assertions.assertNull(TagUtils.findTagValue(tags, "unknown"));

        Assertions.assertEquals(List.of("2026.10", "current"), TagUtils.findTagValues(tags, "RELEASE"));
        Assertions.assertTrue(TagUtils.findTagValues(tags, "unknown").isEmpty());
    }

    @Test
    public void testParseTagCriteria()
    {
        final Map<String, String> criteria = TagUtils.parseTagCriteria("browser: Safari, os: iOS; env=staging, mobile");
        Assertions.assertEquals(4, criteria.size());
        Assertions.assertEquals("Safari", criteria.get("browser"));
        Assertions.assertEquals("iOS", criteria.get("os"));
        Assertions.assertEquals("staging", criteria.get("env"));
        Assertions.assertEquals("", criteria.get("mobile"));

        Assertions.assertTrue(TagUtils.parseTagCriteria(null).isEmpty());
        Assertions.assertTrue(TagUtils.parseTagCriteria("").isEmpty());
        Assertions.assertTrue(TagUtils.parseTagCriteria("   ").isEmpty());
        Assertions.assertTrue(TagUtils.parseTagCriteria("ALL").isEmpty());
    }

    @Test
    public void testMatchesTagCriteriaSingleAndCombinations()
    {
        final Map<String, List<String>> tags = Map.of(
            "env", List.of("staging"),
            "device", List.of("mobile")
        );
        final List<String> junitTags = List.of("smoke", "checkout");

        // Unconditional / empty
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", null));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", ""));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "ALL"));

        // Single key:value match from dynamic tags
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "env: staging"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "ENV: Staging"));

        // Single key:value mismatch
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "env: prod"));

        // Built-in dimension fallback (browser, location)
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome_headless", "US", "browser: Chrome"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Safari", "DE", "locale: DE"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "browser: Firefox"));

        // Conjunction / combination of tags (AND)
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "env: staging, device: mobile"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "env: staging; browser: Chrome"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "env: staging, browser: Safari"));

        // Standalone flag tag (dynamic tag, JUnit tag, or browser/locale)
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "smoke"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "mobile"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "regression"));
    }

    @Test
    public void testMatchesTagCriteriaWithEnvironmentAndBatch()
    {
        final Map<String, List<String>> tags = Map.of(
            "Area", List.of("Checkout")
        );
        final List<String> junitTags = List.of("Smoke");

        // Match environment dimension
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "environment: STG"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "env: stg"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "instance: STG"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "environment: PROD"));

        // Match batch dimension
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "batch: stg-emea"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "suite: stg-emea"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "batch: nightly-us"));

        // Combined criteria: location: DE, environment: STG
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "location: DE, environment: STG"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "location: DE, environment: STG, browser: Chrome"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "US", "STG", "stg-emea", "location: DE, environment: STG"));
        Assertions.assertFalse(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "PROD", "stg-emea", "location: DE, environment: STG"));

        // Flag tag matching environment or batch
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "STG"));
        Assertions.assertTrue(TagUtils.matchesTagCriteria(tags, junitTags, "Chrome", "DE", "STG", "stg-emea", "stg-emea"));
    }
}
