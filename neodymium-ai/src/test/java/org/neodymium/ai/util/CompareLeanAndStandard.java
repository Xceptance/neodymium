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
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares the exact character size and content of LEAN vs STANDARD DOM captures.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class CompareLeanAndStandard
{
    public static void main(final String[] args) throws IOException
    {
        final String logPath = args.length > 0 ? args[0] : "neodymium-ai.testCheckoutLiveAllDataSets.recording.new-dom.log";
        final List<String> lines = Files.readAllLines(Paths.get(logPath));

        final Pattern domCapturePat = Pattern.compile("🔴 \\[DOM Capture:\\s*([A-Z_]+)\\]");
        final Pattern domSizePat = Pattern.compile("📄 Simplified DOM size:\\s*(\\d+)\\s*chars");

        System.out.println("====================================================================================================");
        System.out.println("                 LEAN vs STANDARD DOM CAPTURE COMPARISON");
        System.out.println("====================================================================================================");

        for (int i = 0; i < lines.size(); i++)
        {
            final String line = lines.get(i);
            final Matcher m = domCapturePat.matcher(line);
            if (m.find())
            {
                final String mode = m.group(1);
                String sizeStr = "Unknown";
                if (i + 1 < lines.size())
                {
                    final Matcher sizeMatcher = domSizePat.matcher(lines.get(i + 1));
                    if (sizeMatcher.find())
                    {
                        sizeStr = sizeMatcher.group(1) + " chars";
                    }
                }
                System.out.printf("Line %5d | Mode: %-12s | Size: %s%n", i, mode, sizeStr);
            }
        }
        System.out.println("====================================================================================================");
    }
}
