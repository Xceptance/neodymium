/*
 * MIT License
 *
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
package org.neodymium.license;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural safety net enforcing that Neodymium Classic is 100% pure MIT-licensed
 * and contains zero copyleft (GPL/AGPL) contamination or AI engine dependencies.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class ClassicLicenseComplianceTest
{
    private static final String EXPECTED_MIT_PREFIX = "/*\n * MIT License";
    private static final List<String> FORBIDDEN_AI_PACKAGES = List.of(
        "org.neodymium.ai",
        "com.xceptance.neodymium.ai",
        "com.xceptance.neodymium.aura",
        "dev.langchain4j"
    );

    @Test
    @DisplayName("Verify Langchain4j is not on classpath of neodymium-classic")
    public void testLangchain4jNotOnClasspath()
    {
        Assertions.assertThrows(
            ClassNotFoundException.class,
            () -> Class.forName("dev.langchain4j.model.chat.ChatLanguageModel"),
            "Langchain4j must never be on the classpath of neodymium-classic."
        );
    }

    @Test
    @DisplayName("Verify zero AI imports in neodymium-classic main sources")
    public void testZeroAiImportsInClassicMainSources() throws IOException
    {
        final Path classicRoot = Files.exists(Path.of("src/main/java")) ? Path.of(".") : Path.of("neodymium-classic");
        final Path mainJava = classicRoot.resolve("src/main/java");

        if (!Files.isDirectory(mainJava))
        {
            return;
        }

        final List<String> violations = new ArrayList<>();
        try (final Stream<Path> paths = Files.walk(mainJava))
        {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(javaFile ->
            {
                try
                {
                    final List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
                    for (int lineNum = 0; lineNum < lines.size(); lineNum++)
                    {
                        final String line = lines.get(lineNum).trim();
                        if (line.startsWith("import "))
                        {
                            for (final String forbidden : FORBIDDEN_AI_PACKAGES)
                            {
                                if (line.contains(forbidden))
                                {
                                    violations.add(javaFile + ":" + (lineNum + 1) + " -> " + line);
                                }
                            }
                        }
                    }
                }
                catch (final IOException e)
                {
                    throw new RuntimeException("Failed reading " + javaFile, e);
                }
            });
        }

        Assertions.assertTrue(
            violations.isEmpty(),
            "Found forbidden AI / Langchain4j imports in neodymium-classic main sources:\n" + String.join("\n", violations)
        );
    }

    @Test
    @DisplayName("Assert that 100% of Java files in neodymium-classic carry canonical MIT header and zero copyleft notices")
    public void testZeroCopyleftHeadersAndFullMitCoverage() throws IOException
    {
        final Path classicRoot = Files.exists(Path.of("src/main/java")) ? Path.of(".") : Path.of("neodymium-classic");
        final Path mainJava = classicRoot.resolve("src/main/java");
        final Path testJava = classicRoot.resolve("src/test/java");

        final List<String> missingMitHeader = new ArrayList<>();
        final List<String> copyleftContaminated = new ArrayList<>();

        for (final Path srcDir : List.of(mainJava, testJava))
        {
            if (!Files.isDirectory(srcDir))
            {
                continue;
            }
            try (final Stream<Path> paths = Files.walk(srcDir))
            {
                paths.filter(p -> p.toString().endsWith(".java")).forEach(javaFile ->
                {
                    final String fileName = javaFile.getFileName().toString();
                    if ("JdkHttpClient.java".equals(fileName))
                    {
                        // Verified upstream Selenium fork under Apache 2.0 with Section 4(b) modification notice
                        return;
                    }

                    try
                    {
                        final String content = Files.readString(javaFile, StandardCharsets.UTF_8);
                        final String normalized = content.replace("\r\n", "\n").stripLeading();

                        if (!normalized.startsWith(EXPECTED_MIT_PREFIX))
                        {
                            missingMitHeader.add(javaFile.toString());
                        }

                        final int headerEnd = normalized.indexOf("*/");
                        final String header = (headerEnd > 0) ? normalized.substring(0, headerEnd) : "";

                        if (header.contains("GNU Affero General Public License")
                            || header.contains("AGPL")
                            || header.contains("General Public License"))
                        {
                            copyleftContaminated.add(javaFile.toString());
                        }
                    }
                    catch (final IOException e)
                    {
                        throw new RuntimeException("Failed reading " + javaFile, e);
                    }
                });
            }
        }

        Assertions.assertTrue(
            missingMitHeader.isEmpty(),
            "Found Java files in neodymium-classic without standard MIT license header:\n" + String.join("\n", missingMitHeader)
        );

        Assertions.assertTrue(
            copyleftContaminated.isEmpty(),
            "Found copyleft (GPL/AGPL) license references inside neodymium-classic:\n" + String.join("\n", copyleftContaminated)
        );
    }

    @Test
    @DisplayName("Assert that neodymium-classic pom.xml explicitly declares MIT License")
    public void testPomDeclaresMitLicense() throws IOException
    {
        final Path pomPath = Files.exists(Path.of("src/main/java")) ? Path.of("pom.xml") : Path.of("neodymium-classic/pom.xml");
        Assertions.assertTrue(Files.exists(pomPath), "neodymium-classic pom.xml must exist at: " + pomPath);

        final String pomContent = Files.readString(pomPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(
            pomContent.contains("MIT License"),
            "neodymium-classic pom.xml must declare MIT License"
        );
    }
}
