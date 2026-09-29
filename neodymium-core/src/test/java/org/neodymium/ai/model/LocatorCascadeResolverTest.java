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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Isolated unit tests for the {@link LocatorCascadeResolver} similarity math engine,
 * verifying Jaccard attribute overlap, Levenshtein string distance, and semantic tag equivalence bucketing.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class LocatorCascadeResolverTest
{
    @Test
    public void testExactMatchScore()
    {
        final DomFeatureVector vector = new DomFeatureVector(
            "button",
            "Submit Order",
            Set.of("btn", "btn-primary"),
            Map.of("type", "submit", "name", "order"),
            "button",
            "Submit Order",
            "form",
            0
        );

        final double similarity = LocatorCascadeResolver.computeSimilarity(vector, vector);
        Assertions.assertEquals(1.0, similarity, 0.001);
    }

    @Test
    public void testTailwindClassDriftRecovery()
    {
        // Recorded with Bootstrap classes
        final DomFeatureVector recorded = new DomFeatureVector(
            "button",
            "Add to Shopping Bag",
            Set.of("btn", "btn-primary", "btn-lg", "shadow-sm"),
            Map.of("type", "submit", "name", "add-to-cart"),
            "button",
            "Add to Shopping Bag",
            "form",
            1
        );

        // Live page with Tailwind classes and shortened text
        final DomFeatureVector liveTailwind = new DomFeatureVector(
            "button",
            "Add to Bag",
            Set.of("bg-blue-600", "hover:bg-blue-700", "text-white", "font-bold", "py-2", "px-4", "rounded", "shadow-sm"),
            Map.of("type", "submit", "name", "add-to-cart"),
            "button",
            "Add to Bag",
            "form",
            1
        );

        final double similarity = LocatorCascadeResolver.computeSimilarity(recorded, liveTailwind);
        Assertions.assertTrue(similarity >= 0.80, "Expected similarity >= 0.80 for Tailwind class drift, but was: " + similarity);
    }

    @Test
    public void testSemanticTagEquivalenceBucketing()
    {
        // Recorded as <button>
        final DomFeatureVector recordedButton = new DomFeatureVector(
            "button",
            "Save Changes",
            Set.of("btn"),
            Map.of("name", "save"),
            "button",
            "Save Changes",
            "div",
            0
        );

        // Refactored to <a role="button">
        final DomFeatureVector liveAnchor = new DomFeatureVector(
            "a",
            "Save Changes",
            Set.of("btn"),
            Map.of("name", "save"),
            "button",
            "Save Changes",
            "div",
            0
        );

        final double similarity = LocatorCascadeResolver.computeSimilarity(recordedButton, liveAnchor);
        Assertions.assertTrue(similarity >= 0.85, "Expected similarity >= 0.85 for semantic button-to-link transition, but was: " + similarity);
    }

    @Test
    public void testAmbiguousButtonDisambiguation()
    {
        final DomFeatureVector recordedSave1 = new DomFeatureVector(
            "button",
            "Save",
            Set.of("btn"),
            Map.of("id", "top-save", "type", "submit"),
            "button",
            "Save",
            "header",
            0
        );

        final DomFeatureVector candidate1 = new DomFeatureVector(
            "button",
            "Save",
            Set.of("btn"),
            Map.of("id", "top-save-new", "type", "submit"),
            "button",
            "Save",
            "header",
            0
        );

        final DomFeatureVector candidate2 = new DomFeatureVector(
            "button",
            "Save",
            Set.of("btn"),
            Map.of("id", "footer-save", "type", "submit"),
            "button",
            "Save",
            "footer",
            5
        );

        final DomFeatureVector best = LocatorCascadeResolver.findBestMatch(recordedSave1, List.of(candidate1, candidate2), 0.80);
        Assertions.assertNotNull(best);
        Assertions.assertEquals(candidate1, best);
    }

    @Test
    public void testBelowThresholdRejection()
    {
        final DomFeatureVector recorded = new DomFeatureVector(
            "button",
            "Proceed to Checkout",
            Set.of("btn-checkout"),
            Map.of("id", "chk-btn"),
            "button",
            "Proceed to Checkout",
            "form",
            0
        );

        final DomFeatureVector completelyDifferent = new DomFeatureVector(
            "input",
            "",
            Set.of("form-control"),
            Map.of("type", "text", "placeholder", "Search..."),
            "textbox",
            "Search",
            "nav",
            0
        );

        final DomFeatureVector match = LocatorCascadeResolver.findBestMatch(recorded, List.of(completelyDifferent), 0.80);
        Assertions.assertNull(match, "Expected no match for completely unrelated element");
    }

    @Test
    public void testLevenshteinSimilarity()
    {
        Assertions.assertEquals(1.0, LocatorCascadeResolver.calculateLevenshteinSimilarity("Cart", "Cart"), 0.001);
        Assertions.assertEquals(0.0, LocatorCascadeResolver.calculateLevenshteinSimilarity("", "Cart"), 0.001);
        Assertions.assertTrue(LocatorCascadeResolver.calculateLevenshteinSimilarity("Shopping Cart", "Shopping Bag") >= 0.50);
    }

    @Test
    public void testLevenshteinWithDuplicateWordsNoCrash()
    {
        // Must not throw IllegalArgumentException: duplicate element
        final double score = LocatorCascadeResolver.calculateLevenshteinSimilarity(
            "add to cart add to cart",
            "add to cart"
        );
        Assertions.assertTrue(score > 0.0, "Expected non-zero similarity for repeated word tokens");
    }

    @Test
    public void testJaccardSetSimilarity()
    {
        Assertions.assertEquals(1.0, LocatorCascadeResolver.calculateJaccard(Set.of("a", "b"), Set.of("a", "b")), 0.001);
        Assertions.assertEquals(0.0, LocatorCascadeResolver.calculateJaccard(Set.of("a"), Set.of("b")), 0.001);
        Assertions.assertEquals(1.0 / 3.0, LocatorCascadeResolver.calculateJaccard(Set.of("a", "b"), Set.of("b", "c")), 0.001);
        Assertions.assertEquals(0.0, LocatorCascadeResolver.calculateJaccard(Set.of(), Set.of()), 0.001);
    }

    @Test
    public void testTagEquivalenceBucketRestrictions()
    {
        // button vs a with button role -> 0.95
        Assertions.assertEquals(0.95, LocatorCascadeResolver.calculateTagScore("button", "a", "button", "button"), 0.001);

        // button vs a without role -> 0.70
        Assertions.assertEquals(0.70, LocatorCascadeResolver.calculateTagScore("button", "a", null, null), 0.001);

        // div vs span without interactive roles -> 0.0
        Assertions.assertEquals(0.0, LocatorCascadeResolver.calculateTagScore("div", "span", null, null), 0.001);

        // div role="button" vs span role="button" -> 0.95
        Assertions.assertEquals(0.95, LocatorCascadeResolver.calculateTagScore("div", "span", "button", "button"), 0.001);
    }

    @Test
    public void testExactOpenSpecWeightsFormula()
    {
        // Tag match (1.0 * 0.35 = 0.35)
        // Attr match (1.0 * 0.30 = 0.30)
        // Text mismatch (0.0 * 0.25 = 0.0)
        // Class match (1.0 * 0.10 = 0.10)
        // Expected total = 0.75
        final DomFeatureVector v1 = new DomFeatureVector(
            "button", "Submit", Set.of("btn"), Map.of("id", "btn1"), "button", "Submit", "form", 0
        );
        final DomFeatureVector v2 = new DomFeatureVector(
            "button", "", Set.of("btn"), Map.of("id", "btn1"), "button", "", "form", 0
        );

        final double sim = LocatorCascadeResolver.computeSimilarity(v1, v2);
        Assertions.assertEquals(0.75, sim, 0.001);
    }

    @Test
    public void testDomFeatureVectorBoundingBoxAndConstructors()
    {
        final DomFeatureVector v = new DomFeatureVector(
            "button", "Checkout", Set.of("btn"), Map.of("id", "btn1"), "button", "Checkout", "form", 0, 100, 200, 150, 40
        );

        Assertions.assertEquals(100, v.getX());
        Assertions.assertEquals(200, v.getY());
        Assertions.assertEquals(150, v.getWidth());
        Assertions.assertEquals(40, v.getHeight());
        Assertions.assertEquals("button", v.getTag());
        Assertions.assertEquals("Checkout", v.getText());

        final DomFeatureVector legacy = new DomFeatureVector(
            "button", "Checkout", Set.of("btn"), Map.of("id", "btn1"), "button", "Checkout", "form", 0
        );
        Assertions.assertEquals(0, legacy.getX());
        Assertions.assertEquals(0, legacy.getY());
        Assertions.assertEquals(0, legacy.getWidth());
        Assertions.assertEquals(0, legacy.getHeight());

        final DomFeatureVector same = new DomFeatureVector(
            "button", "Checkout", Set.of("btn"), Map.of("id", "btn1"), "button", "Checkout", "form", 0, 100, 200, 150, 40
        );
        Assertions.assertEquals(v, same);
        Assertions.assertEquals(v.hashCode(), same.hashCode());
        Assertions.assertNotEquals(v, legacy);
        Assertions.assertTrue(v.toString().contains("x=100"));
        Assertions.assertTrue(v.toString().contains("attributes={id=btn1}"));
        Assertions.assertTrue(v.toDetailString().contains("tag=<button>"));
        Assertions.assertTrue(v.toDetailString().contains("text='Checkout'"));
        Assertions.assertTrue(v.toDetailString().contains("attrs={id=btn1}"));
        Assertions.assertTrue(v.toDetailString().contains("bounds=(100, 200, 150x40)"));
        Assertions.assertEquals("<button> \"Checkout\" parent=<form>#0 (100, 200, 150x40)", v.toSummaryString());
        final java.util.List<String> lines = v.toFormattedLines("   │ Vector: ", "   │         ");
        Assertions.assertFalse(lines.isEmpty());
        Assertions.assertTrue(lines.get(0).startsWith("   │ Vector: tag=<button>"));
    }

    @Test
    public void testVisualDHashSimilarityExactAndClose()
    {
        final String hashA = "ffff0000ffff0000";
        final String hashB = "ffff0000ffff0000";
        final String hashC = "ffff0000ffff0003"; // 2 bits different

        Assertions.assertEquals(1.0, LocatorCascadeResolver.calculateDHashSimilarity(hashA, hashB), 0.001);
        final double closeSim = LocatorCascadeResolver.calculateDHashSimilarity(hashA, hashC);
        Assertions.assertTrue(closeSim >= 0.95, "2 bits different should be >= 0.95 similarity: " + closeSim);
    }

    @Test
    public void testIconOnlyElementMatchingViaVisualDHashAndAspect()
    {
        // Recorded icon-only button: text is empty, has visual dHash and 40x40 bounding box
        final DomFeatureVector recordedIcon = new DomFeatureVector(
            "button",
            "",
            Set.of("icon-btn", "btn-cart"),
            Map.of("type", "button", "aria-label", ""),
            "button",
            "",
            "header",
            2,
            500,
            20,
            40,
            40,
            "a1b2c3d4e5f60718",
            ""
        );

        // Candidate 1 on live page: refactored classes, but identical aspect ratio and close visual dHash
        final DomFeatureVector liveMatch = new DomFeatureVector(
            "button",
            "",
            Set.of("btn", "action-icon"),
            Map.of("type", "button"),
            "button",
            "",
            "header",
            2,
            520,
            22,
            40,
            40,
            "a1b2c3d4e5f60719", // 1 bit different
            ""
        );

        // Candidate 2 on live page: completely different aspect ratio and visual dHash
        final DomFeatureVector liveDifferent = new DomFeatureVector(
            "button",
            "",
            Set.of("btn", "text-btn"),
            Map.of("type", "button"),
            "button",
            "",
            "header",
            1,
            100,
            20,
            120,
            40,
            "0000000000000000",
            ""
        );

        final double matchScore = LocatorCascadeResolver.computeSimilarity(recordedIcon, liveMatch);
        final double diffScore = LocatorCascadeResolver.computeSimilarity(recordedIcon, liveDifferent);

        Assertions.assertTrue(matchScore >= 0.85, "Icon-only match score must be >= 0.85, was: " + matchScore);
        Assertions.assertTrue(matchScore > diffScore, "Matching candidate must score higher than different candidate");

        final DomFeatureVector best = LocatorCascadeResolver.findBestMatch(recordedIcon, List.of(liveDifferent, liveMatch), 0.85);
        Assertions.assertNotNull(best);
        Assertions.assertEquals(liveMatch, best);
    }
}
