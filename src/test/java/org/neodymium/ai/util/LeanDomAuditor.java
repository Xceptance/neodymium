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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neodymium.ai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Audits the actual DOM dumps sent to the LLM under LEAN context level
 * in log files to evaluate whether the claim "interactive elements only" is accurate.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class LeanDomAuditor
{
    public static void main(final String[] args)
    {
        final String logPath = args.length > 0 ? args[0] : "neodymium-ai.testCheckoutLiveAllDataSets.recording.new-dom.log";

        try
        {
            auditLogFile(logPath);
        }
        catch (final Exception e)
        {
            System.err.println("Error auditing LEAN DOM dumps: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void auditLogFile(final String logPath) throws IOException
    {
        final Path path = Paths.get(logPath);
        if (!Files.exists(path))
        {
            System.err.println("File not found: " + logPath);
            return;
        }

        final List<String> lines = Files.readAllLines(path);
        System.out.println("====================================================================================================");
        System.out.println("                         LEAN DOM CONTEXT AUDIT REPORT");
        System.out.println("====================================================================================================");
        System.out.println("Log File: " + logPath);
        System.out.println("Total Log Lines: " + lines.size());
        System.out.println("----------------------------------------------------------------------------------------------------");

        final Pattern modePat = Pattern.compile("\\[DOM Capture:\\s*([A-Z_]+)\\]");
        final Pattern htmlTagPat = Pattern.compile("<([a-zA-Z0-9-]+)");

        final Map<String, Integer> modeCounts = new LinkedHashMap<>();
        final Map<String, Integer> leanTagCounts = new LinkedHashMap<>();
        final Map<String, Integer> standardTagCounts = new LinkedHashMap<>();

        final List<String> sampleLeanLines = new ArrayList<>();
        final List<String> staticTextElementsFoundInLean = new ArrayList<>();

        String activeMode = null;
        boolean readingDom = false;

        for (int i = 0; i < lines.size(); i++)
        {
            final String line = lines.get(i);

            final Matcher m = modePat.matcher(line);
            if (m.find())
            {
                activeMode = m.group(1);
                modeCounts.put(activeMode, modeCounts.getOrDefault(activeMode, 0) + 1);
                readingDom = true;
                continue;
            }

            if (readingDom)
            {
                // DOM lines usually appear in TRACE / DEBUG logs or inside attachments
                if (line.contains("Compiling prompt:") || line.contains("System Prompt:") || line.contains("LLM response received") || line.contains("=========="))
                {
                    // stop reading for this step
                    if (line.contains("Compiling prompt:") || line.contains("LLM response received"))
                    {
                        readingDom = false;
                    }
                }
                else
                {
                    // Look for HTML tags
                    final Matcher tagMatcher = htmlTagPat.matcher(line);
                    while (tagMatcher.find())
                    {
                        final String tag = tagMatcher.group(1).toLowerCase();
                        if (activeMode != null && activeMode.equals("LEAN"))
                        {
                            leanTagCounts.put(tag, leanTagCounts.getOrDefault(tag, 0) + 1);
                        }
                        else if (activeMode != null && activeMode.equals("STANDARD"))
                        {
                            standardTagCounts.put(tag, standardTagCounts.getOrDefault(tag, 0) + 1);
                        }
                    }

                    final String trimmed = line.trim();
                    if (trimmed.startsWith("<") && activeMode != null && activeMode.equals("LEAN"))
                    {
                        if (sampleLeanLines.size() < 30)
                        {
                            sampleLeanLines.add(trimmed);
                        }

                        // Check if tag is non-interactive static text
                        if ((trimmed.startsWith("<p") || trimmed.startsWith("<span") || trimmed.startsWith("<div") || trimmed.startsWith("<li") || trimmed.startsWith("<td") || trimmed.startsWith("<h"))
                            && !trimmed.contains("data-ai") && !trimmed.contains("onclick") && !trimmed.contains("cursor") && !trimmed.contains("type=") && !trimmed.contains("role=") && !trimmed.contains("href="))
                        {
                            if (staticTextElementsFoundInLean.size() < 20)
                            {
                                staticTextElementsFoundInLean.add(trimmed);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("1. DOM CAPTURE FREQUENCY BY MODE");
        System.out.println("----------------------------------------------------------------------------------------------------");
        for (final Map.Entry<String, Integer> entry : modeCounts.entrySet())
        {
            System.out.printf("  - Mode [%-15s]: %d occurrences%n", entry.getKey(), entry.getValue());
        }

        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("2. HTML TAG DISTRIBUTION IN LEAN PAYLOADS");
        System.out.println("----------------------------------------------------------------------------------------------------");
        if (leanTagCounts.isEmpty())
        {
            System.out.println("  (Tag parsing check: inspecting DOM lines)");
        }
        for (final Map.Entry<String, Integer> entry : leanTagCounts.entrySet())
        {
            System.out.printf("  - <%s>: %d occurrences%n", entry.getKey(), entry.getValue());
        }

        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("3. AUDIT OF NON-INTERACTIVE / STRUCTURAL ELEMENTS IN LEAN");
        System.out.println("----------------------------------------------------------------------------------------------------");
        if (staticTextElementsFoundInLean.isEmpty())
        {
            System.out.println("  ✓ Only interactive elements / containers found in LEAN payloads!");
        }
        else
        {
            System.out.println("  ⚠️ Non-interactive elements or containers present in LEAN mode:");
            for (int k = 0; k < Math.min(20, staticTextElementsFoundInLean.size()); k++)
            {
                System.out.println("    [" + (k + 1) + "] " + truncate(staticTextElementsFoundInLean.get(k), 120));
            }
        }

        System.out.println("\n----------------------------------------------------------------------------------------------------");
        System.out.println("4. SAMPLE LEAN DOM ELEMENT LINES");
        System.out.println("----------------------------------------------------------------------------------------------------");
        for (int j = 0; j < Math.min(25, sampleLeanLines.size()); j++)
        {
            System.out.println("  " + sampleLeanLines.get(j));
        }

        System.out.println("====================================================================================================");
    }

    private static String truncate(final String str, final int maxLen)
    {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }
}
