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

import org.neodymium.ai.util.ScreenshotHasher;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computational engine for evaluating DOM feature vector similarity during replay,
 * executing sub-millisecond local self-healing across frontend refactoring.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class LocatorCascadeResolver
{
    private LocatorCascadeResolver()
    {
    }

    /**
     * Computes the composite similarity score (0.0 to 1.0) between a recorded target vector
     * and a live candidate element vector using the exact OpenSpec formula:
     * 0.35 * TagScore + 0.30 * AttrJaccard + 0.25 * TextLevenshtein + 0.10 * ClassJaccard.
     * When text and accessible names are absent (e.g. icon-only elements), Tier 2 visual
     * perceptual dHash and aspect ratio similarity are evaluated.
     *
     * @param recorded the baseline recorded feature vector
     * @param candidate the live candidate feature vector
     * @return composite similarity score between 0.0 and 1.0
     */
    public static double computeSimilarity(final DomFeatureVector recorded, final DomFeatureVector candidate)
    {
        if (recorded == null || candidate == null)
        {
            return 0.0;
        }

        // 1. Tag & Role Score (Weight: 35%)
        final double tagScore = calculateTagScore(recorded.getTag(), candidate.getTag(), recorded.getRole(), candidate.getRole());

        // 2. Attributes Jaccard Overlap (Weight: 30%)
        final double attrScore = calculateAttributeJaccard(recorded.getAttributes(), candidate.getAttributes());

        // Check if element is icon-only or textless with visual similarity metadata
        final boolean isTextless = (recorded.getText() == null || recorded.getText().isBlank())
            && (recorded.getAccessibleName() == null || recorded.getAccessibleName().isBlank());

        if (isTextless && (recorded.hasVisualHash() || (recorded.getTileSsim() != null && !recorded.getTileSsim().isBlank())))
        {
            // Tier 2 Perceptual Visual Similarity (Aspect ratio + dHash / Tile SSIM)
            final double visualScore = computeVisualSimilarity(recorded, candidate);
            final double classScore = calculateJaccard(recorded.getClasses(), candidate.getClasses());

            // 0.35 * TagScore + 0.15 * AttrJaccard + 0.45 * VisualScore + 0.05 * ClassJaccard
            return (tagScore * 0.35) + (attrScore * 0.15) + (visualScore * 0.45) + (classScore * 0.05);
        }

        // 3. Text & Accessible Name Score (Weight: 25%)
        final double textScore = Math.max(
            calculateLevenshteinSimilarity(recorded.getText(), candidate.getText()),
            calculateLevenshteinSimilarity(recorded.getAccessibleName(), candidate.getAccessibleName())
        );

        // 4. Classes Jaccard Overlap (Weight: 10%)
        final double classScore = calculateJaccard(recorded.getClasses(), candidate.getClasses());

        return (tagScore * 0.35) + (attrScore * 0.30) + (textScore * 0.25) + (classScore * 0.10);
    }

    /**
     * Finds the candidate with the highest similarity score exceeding the threshold.
     * Uses structural hierarchy (parentTag, siblingIndex) as a deterministic tie-breaker.
     *
     * @param target the target vector to match
     * @param candidates the list of candidate vectors on the live page
     * @param threshold the minimum required similarity threshold (default: 0.80)
     * @return the best matching candidate, or {@code null} if no candidate meets the threshold
     */
    public static DomFeatureVector findBestMatch(
        final DomFeatureVector target,
        final List<DomFeatureVector> candidates,
        final double threshold)
    {
        if (target == null || candidates == null || candidates.isEmpty())
        {
            return null;
        }

        DomFeatureVector bestMatch = null;
        double highestScore = -1.0;

        for (final DomFeatureVector candidate : candidates)
        {
            if (candidate == null)
            {
                continue;
            }
            final double baseScore = computeSimilarity(target, candidate);
            if (baseScore >= (threshold - 1e-5))
            {
                // Structural tie-breaker bonus
                double tieBreaker = 0.0;
                if (target.getParentTag() != null && candidate.getParentTag() != null
                    && target.getParentTag().equalsIgnoreCase(candidate.getParentTag()))
                {
                    tieBreaker += 0.005;
                }
                final int indexDiff = Math.abs(target.getSiblingIndex() - candidate.getSiblingIndex());
                tieBreaker += Math.max(0.0, 0.005 * (1.0 - (indexDiff / 10.0)));

                final double totalScore = baseScore + tieBreaker;
                if (totalScore > highestScore)
                {
                    highestScore = totalScore;
                    bestMatch = candidate;
                }
            }
        }

        return bestMatch;
    }

    /**
     * Resolves the effective ARIA role for an element, combining explicit role attributes
     * with standard HTML tag implicit roles (e.g. {@code <button>} -> {@code "button"}).
     *
     * @param tag the HTML tag name
     * @param role the explicit ARIA role attribute, if any
     * @return the normalized effective ARIA role, or empty string if unspecified
     */
    public static String getEffectiveRole(final String tag, final String role)
    {
        if (role != null && !role.isBlank())
        {
            return role.trim().toLowerCase();
        }
        if (tag == null)
        {
            return "";
        }
        final String t = tag.trim().toLowerCase();
        return switch (t)
        {
            case "button" -> "button";
            case "a" -> "link";
            case "input", "textarea" -> "textbox";
            case "select" -> "combobox";
            case "nav" -> "navigation";
            case "header" -> "banner";
            case "footer" -> "contentinfo";
            case "main" -> "main";
            default -> "";
        };
    }

    /**
     * Calculates tag and ARIA role compatibility with semantic equivalence bucketing.
     */
    public static double calculateTagScore(
        final String recordedTag,
        final String candidateTag,
        final String recordedRole,
        final String candidateRole)
    {
        if (recordedTag == null || candidateTag == null)
        {
            return 0.0;
        }

        if (recordedTag.equalsIgnoreCase(candidateTag))
        {
            return 1.0;
        }

        final String effRecordedRole = getEffectiveRole(recordedTag, recordedRole);
        final String effCandidateRole = getEffectiveRole(candidateTag, candidateRole);

        if (!effRecordedRole.isBlank() && !effCandidateRole.isBlank()
            && effRecordedRole.equalsIgnoreCase(effCandidateRole))
        {
            return 0.95;
        }

        final boolean recordedIsInteractive = isInteractiveElement(recordedTag, recordedRole);
        final boolean candidateIsInteractive = isInteractiveElement(candidateTag, candidateRole);

        if (recordedIsInteractive && candidateIsInteractive)
        {
            return 0.70;
        }

        return 0.0;
    }

    private static boolean isInteractiveElement(final String tag, final String role)
    {
        if (tag == null)
        {
            return false;
        }
        final String t = tag.trim().toLowerCase();
        if ("button".equals(t) || "a".equals(t) || "input".equals(t))
        {
            return true;
        }
        if (role != null)
        {
            final String r = role.trim().toLowerCase();
            return "button".equals(r) || "link".equals(r) || "checkbox".equals(r) || "tab".equals(r) || "menuitem".equals(r) || "submit".equals(r);
        }
        return false;
    }

    /**
     * Calculates Jaccard set similarity over two sets of strings.
     */
    public static double calculateJaccard(final Set<String> setA, final Set<String> setB)
    {
        if (setA == null || setB == null || setA.isEmpty() || setB.isEmpty())
        {
            return 0.0;
        }

        final Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty())
        {
            return 0.0;
        }

        int intersectionCount = 0;
        for (final String item : setA)
        {
            if (setB.contains(item))
            {
                intersectionCount++;
            }
        }

        return (double) intersectionCount / union.size();
    }

    /**
     * Calculates Jaccard similarity over key-value attribute entries.
     */
    private static double calculateAttributeJaccard(final Map<String, String> mapA, final Map<String, String> mapB)
    {
        if (mapA == null || mapB == null || mapA.isEmpty() || mapB.isEmpty())
        {
            return 0.0;
        }

        final Set<String> entriesA = new HashSet<>();
        for (final Map.Entry<String, String> entry : mapA.entrySet())
        {
            entriesA.add(entry.getKey() + "=" + entry.getValue());
        }

        final Set<String> entriesB = new HashSet<>();
        for (final Map.Entry<String, String> entry : mapB.entrySet())
        {
            entriesB.add(entry.getKey() + "=" + entry.getValue());
        }

        return calculateJaccard(entriesA, entriesB);
    }

    /**
     * Calculates normalized string similarity combining Levenshtein distance and word token overlap.
     */
    public static double calculateLevenshteinSimilarity(final String s1, final String s2)
    {
        if (s1 == null || s2 == null)
        {
            return 0.0;
        }
        final String str1 = s1.trim().toLowerCase();
        final String str2 = s2.trim().toLowerCase();

        if (str1.equals(str2))
        {
            return 1.0;
        }
        if (str1.isEmpty() || str2.isEmpty())
        {
            return 0.0;
        }

        // 1. Character-level Levenshtein
        final int distance = computeLevenshteinDistance(str1, str2);
        final int maxLength = Math.max(str1.length(), str2.length());
        final double charSim = Math.max(0.0, 1.0 - ((double) distance / maxLength));

        // 2. Word Token Jaccard Overlap
        final Set<String> words1 = new HashSet<>(java.util.Arrays.asList(str1.split("\\s+")));
        final Set<String> words2 = new HashSet<>(java.util.Arrays.asList(str2.split("\\s+")));
        final double tokenSim = calculateJaccard(words1, words2);

        return Math.max(charSim, tokenSim);
    }

    private static int computeLevenshteinDistance(final String s1, final String s2)
    {
        final int[] prev = new int[s2.length() + 1];
        final int[] curr = new int[s2.length() + 1];

        for (int j = 0; j <= s2.length(); j++)
        {
            prev[j] = j;
        }

        for (int i = 1; i <= s1.length(); i++)
        {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++)
            {
                final int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            System.arraycopy(curr, 0, prev, 0, curr.length);
        }

        return prev[s2.length()];
    }

    /**
     * Computes perceptual visual similarity combining bounding box aspect ratio and visual dHash (or tile SSIM).
     *
     * @param recorded baseline recorded vector
     * @param candidate candidate live vector
     * @return visual similarity score between 0.0 and 1.0
     */
    public static double computeVisualSimilarity(final DomFeatureVector recorded, final DomFeatureVector candidate)
    {
        if (recorded == null || candidate == null)
        {
            return 0.0;
        }

        final double aspectSim = calculateAspectSimilarity(recorded, candidate);

        double hashSim = -1.0;
        if (recorded.hasVisualHash() && candidate.hasVisualHash())
        {
            hashSim = calculateDHashSimilarity(recorded.getVisualHash(), candidate.getVisualHash());
        }
        else if (recorded.getTileSsim() != null && !recorded.getTileSsim().isBlank()
            && candidate.getTileSsim() != null && !candidate.getTileSsim().isBlank())
        {
            hashSim = ScreenshotHasher.calculateSsim(recorded.getTileSsim(), candidate.getTileSsim());
        }

        if (hashSim >= 0.0)
        {
            return (hashSim * 0.75) + (aspectSim * 0.25);
        }

        return aspectSim;
    }

    /**
     * Computes aspect ratio similarity between two bounding boxes.
     *
     * @param recorded baseline recorded vector
     * @param candidate candidate live vector
     * @return aspect ratio similarity between 0.0 and 1.0
     */
    public static double calculateAspectSimilarity(final DomFeatureVector recorded, final DomFeatureVector candidate)
    {
        if (recorded == null || candidate == null)
        {
            return 0.0;
        }
        if (recorded.getWidth() <= 0 || recorded.getHeight() <= 0
            || candidate.getWidth() <= 0 || candidate.getHeight() <= 0)
        {
            return 1.0;
        }

        final double rAspect = (double) recorded.getWidth() / recorded.getHeight();
        final double cAspect = (double) candidate.getWidth() / candidate.getHeight();
        final double diff = Math.abs(rAspect - cAspect);
        final double maxAspect = Math.max(rAspect, cAspect);

        return Math.max(0.0, 1.0 - (diff / maxAspect));
    }

    /**
     * Calculates the perceptual similarity score (0.0 to 1.0) between two 64-bit dHash hex strings.
     *
     * @param hash1 first hex dHash
     * @param hash2 second hex dHash
     * @return similarity score between 0.0 and 1.0 (1.0 = identical), or -1.0 if invalid
     */
    public static double calculateDHashSimilarity(final String hash1, final String hash2)
    {
        return ScreenshotHasher.calculateDHashSimilarity(hash1, hash2);
    }
}
