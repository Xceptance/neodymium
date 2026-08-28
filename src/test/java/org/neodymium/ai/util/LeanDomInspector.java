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
import java.util.List;

/**
 * Utility to inspect DOM captures under LEAN mode.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class LeanDomInspector
{
    public static void main(final String[] args) throws IOException
    {
        final String logPath = args.length > 0 ? args[0] : "neodymium-ai.testCheckoutLiveAllDataSets.recording.new-dom.log";
        final List<String> lines = Files.readAllLines(Paths.get(logPath));

        System.out.println("Searching for [DOM Capture: LEAN] in " + logPath + "...");

        for (int i = 0; i < lines.size(); i++)
        {
            final String line = lines.get(i);
            if (line.contains("[DOM Capture: LEAN]"))
            {
                System.out.println("\n----------------------------------------------------------------------------------------------------");
                System.out.println("Found LEAN capture at line " + i + ": " + line);
                System.out.println("----------------------------------------------------------------------------------------------------");

                // Print the next 60 lines to see the actual DOM dump sent to the LLM
                for (int j = i + 1; j < Math.min(i + 60, lines.size()); j++)
                {
                    final String domLine = lines.get(j);
                    if (domLine.contains("Compiling prompt:") || domLine.contains("System Prompt:") || domLine.contains("LLM response received"))
                    {
                        break;
                    }
                    System.out.println("L" + j + ": " + domLine);
                }
                break; // examine first LEAN capture in detail
            }
        }
    }
}
