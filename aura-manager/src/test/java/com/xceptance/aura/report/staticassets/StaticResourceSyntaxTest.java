/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.report.staticassets;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Automated regression safety net verifying that all static JavaScript assets
 * in {@code src/main/resources/static/js/} are syntactically valid.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class StaticResourceSyntaxTest
{
    @Test
    public void testAllStaticJavaScriptAssetsHaveValidSyntax() throws Exception
    {
        final Path jsDirPath = Paths.get("src/main/resources/static/js");
        Assertions.assertTrue(Files.exists(jsDirPath), "Static JS directory must exist: " + jsDirPath.toAbsolutePath());

        final List<File> jsFiles;
        try (final Stream<Path> stream = Files.list(jsDirPath))
        {
            jsFiles = stream
                .filter(p -> p.toString().endsWith(".js"))
                .map(Path::toFile)
                .sorted()
                .collect(Collectors.toList());
        }

        Assertions.assertFalse(jsFiles.isEmpty(), "At least one JavaScript file should be present in " + jsDirPath);

        final boolean nodeAvailable = isNodeAvailable();

        for (final File jsFile : jsFiles)
        {
            Assertions.assertTrue(jsFile.length() > 0, "JavaScript file must not be empty: " + jsFile.getName());

            if (nodeAvailable)
            {
                verifySyntaxWithNode(jsFile);
            }
            else
            {
                verifyBraceBalance(jsFile);
            }
        }
    }

    private boolean isNodeAvailable()
    {
        try
        {
            final Process process = new ProcessBuilder("node", "-v").start();
            final int exitCode = process.waitFor();
            return exitCode == 0;
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    private void verifySyntaxWithNode(final File jsFile) throws Exception
    {
        final ProcessBuilder pb = new ProcessBuilder("node", "-c", jsFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final StringBuilder output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        Assertions.assertEquals(0, exitCode, "Syntax error detected in " + jsFile.getName() + ":\n" + output);
    }

    private void verifyBraceBalance(final File jsFile) throws Exception
    {
        final String content = Files.readString(jsFile.toPath(), StandardCharsets.UTF_8);
        int braceCount = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++)
        {
            final char c = content.charAt(i);

            if (escaped)
            {
                escaped = false;
                continue;
            }

            if (c == '\\')
            {
                escaped = true;
                continue;
            }

            if (inLineComment)
            {
                if (c == '\n')
                {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment)
            {
                if (c == '*' && i + 1 < content.length() && content.charAt(i + 1) == '/')
                {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick)
            {
                if (c == '/' && i + 1 < content.length())
                {
                    final char next = content.charAt(i + 1);
                    if (next == '/')
                    {
                        inLineComment = true;
                        i++;
                        continue;
                    }
                    else if (next == '*')
                    {
                        inBlockComment = true;
                        i++;
                        continue;
                    }
                }
            }

            if (c == '\'' && !inDoubleQuote && !inBacktick)
            {
                inSingleQuote = !inSingleQuote;
            }
            else if (c == '"' && !inSingleQuote && !inBacktick)
            {
                inDoubleQuote = !inDoubleQuote;
            }
            else if (c == '`' && !inSingleQuote && !inDoubleQuote)
            {
                inBacktick = !inBacktick;
            }
            else if (!inSingleQuote && !inDoubleQuote && !inBacktick)
            {
                if (c == '{')
                {
                    braceCount++;
                }
                else if (c == '}')
                {
                    braceCount--;
                }
            }
        }

        Assertions.assertEquals(0, braceCount, "Brace mismatch detected in " + jsFile.getName());
    }
}
