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
 * Architectural safety net enforcing strict module copyleft boundary and isolation.
 * Verifies that neodymium-core has zero compile-time or runtime dependencies on AI packages,
 * Langchain4j, or Aura components, ensuring neodymium-core remains pure MIT.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class LicenseComplianceTest
{
    private static final List<String> FORBIDDEN_PACKAGE_PREFIXES = List.of(
        "org.neodymium.ai",
        "com.xceptance.neodymium.ai",
        "com.xceptance.neodymium.aura",
        "dev.langchain4j"
    );

    @Test
    @DisplayName("Verify Langchain4j is not on classpath of neodymium-core")
    public void testLangchain4jNotOnClasspath()
    {
        Assertions.assertThrows(
            ClassNotFoundException.class,
            () -> Class.forName("dev.langchain4j.model.chat.ChatLanguageModel"),
            "dev.langchain4j.model.chat.ChatLanguageModel must not be present on neodymium-core classpath"
        );
    }

    @Test
    @DisplayName("Verify AI facades and runners are not on classpath of neodymium-core")
    public void testAiClassesNotOnClasspath()
    {
        final List<String> aiClasses = List.of(
            "org.neodymium.ai.NeodymiumAi",
            "org.neodymium.ai.junit.NeodymiumAiRunner",
            "org.neodymium.ai.junit.NeodymiumAiTest",
            "com.xceptance.neodymium.ai.console.InteractiveConsoleEngine",
            "com.xceptance.neodymium.aura.AuraFileService"
        );

        for (final String className : aiClasses)
        {
            Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName(className),
                className + " must not be resolvable in neodymium-core"
            );
        }
    }

    @Test
    @DisplayName("Verify neodymium-core main source tree contains zero AI or Langchain4j imports")
    public void testZeroAiImportsInCoreMainSources() throws IOException
    {
        final Path mainJavaDir = Path.of("src/main/java");
        final Path targetDir = Files.exists(mainJavaDir) ? mainJavaDir : Path.of("neodymium-core/src/main/java");
        Assertions.assertTrue(Files.isDirectory(targetDir), "Main source directory must exist: " + targetDir);

        final List<String> violations = new ArrayList<>();
        try (final Stream<Path> pathStream = Files.walk(targetDir))
        {
            pathStream.filter(p -> p.toString().endsWith(".java")).forEach(javaFile ->
            {
                try
                {
                    final List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++)
                    {
                        final String line = lines.get(i).trim();
                        if (line.startsWith("import "))
                        {
                            for (final String forbidden : FORBIDDEN_PACKAGE_PREFIXES)
                            {
                                if (line.startsWith("import " + forbidden) || line.startsWith("import static " + forbidden))
                                {
                                    violations.add(javaFile + ":" + (i + 1) + " -> " + line);
                                }
                            }
                        }
                    }
                }
                catch (final IOException e)
                {
                    throw new RuntimeException("Failed reading file: " + javaFile, e);
                }
            });
        }

        Assertions.assertTrue(
            violations.isEmpty(),
            "Found forbidden AI/Langchain4j imports in neodymium-core main sources:\n" + String.join("\n", violations)
        );
    }

    @Test
    @DisplayName("Verify neodymium-core built classes contain zero AI or Aura package artifacts")
    public void testZeroAiClassesInTargetClasses() throws IOException
    {
        final Path targetClasses = Path.of("target/classes");
        final Path classesDir = Files.exists(targetClasses) ? targetClasses : Path.of("neodymium-core/target/classes");
        if (!Files.isDirectory(classesDir))
        {
            return;
        }

        final List<String> violations = new ArrayList<>();
        try (final Stream<Path> stream = Files.walk(classesDir))
        {
            stream.filter(p -> p.toString().endsWith(".class")).forEach(classFile ->
            {
                final String relativePath = classesDir.relativize(classFile).toString().replace(File.separatorChar, '.');
                for (final String forbidden : FORBIDDEN_PACKAGE_PREFIXES)
                {
                    if (relativePath.startsWith(forbidden))
                    {
                        violations.add(relativePath);
                    }
                }
            });
        }

        Assertions.assertTrue(
            violations.isEmpty(),
            "Found compiled classes in forbidden AI/Aura packages under neodymium-core:\n" + String.join("\n", violations)
        );
    }
}
