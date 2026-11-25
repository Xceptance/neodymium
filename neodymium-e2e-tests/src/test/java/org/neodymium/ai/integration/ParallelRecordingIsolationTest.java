/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.integration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.junit.AiPlaybook;

/**
 * Verifies that no two {@code @AiPlaybook} test classes resolve to the same
 * {@code (recordingDirectory, baseName)} recording destination during parallel Surefire execution.
 *
 * @author AI-generated: Gemini
 * @author Xceptance GmbH 2026
 */
public class ParallelRecordingIsolationTest
{
    /**
     * Scans all E2E test classes using {@code @AiPlaybook} and asserts zero recording-target collisions.
     *
     * @throws IOException if scanning test source files fails
     * @throws ClassNotFoundException if a scanned test class cannot be loaded
     */
    @Test
    public void testNoAiPlaybookRecordingDirectoryCollisionsAcrossClasses() throws IOException, ClassNotFoundException
    {
        final Path testSourceRoot = resolveTestSourceRoot();
        Assertions.assertTrue(Files.isDirectory(testSourceRoot),
            "Could not locate neodymium-e2e-tests source directory: " + testSourceRoot.toAbsolutePath());

        final List<Path> testFiles;
        try (final Stream<Path> walked = Files.walk(testSourceRoot))
        {
            testFiles = walked
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith("Test.java"))
                .sorted()
                .toList();
        }

        Assertions.assertFalse(testFiles.isEmpty(), "Expected E2E *Test.java files under " + testSourceRoot);

        final Map<String, Set<String>> recordingTargets = new LinkedHashMap<>();
        int scannedPlaybookClasses = 0;

        for (final Path testFile : testFiles)
        {
            final String fqcn = toFqcn(testSourceRoot, testFile);
            final Class<?> testClass = Class.forName(fqcn, false, Thread.currentThread().getContextClassLoader());
            if (Modifier.isAbstract(testClass.getModifiers()) || testClass.isInterface())
            {
                continue;
            }

            final AiPlaybook classAnnotation = testClass.getAnnotation(AiPlaybook.class);
            final String classDir = normalizeBlank(classAnnotation != null ? classAnnotation.recordingDirectory() : null);
            final String classFile = normalizeBlank(classAnnotation != null ? classAnnotation.recordingFileName() : null);

            boolean hasPlaybookAnnotation = false;
            if (classAnnotation != null)
            {
                hasPlaybookAnnotation = true;
                final String classKey = (classDir != null ? classDir : "<default>")
                    + "::" + (classFile != null ? classFile : testClass.getSimpleName());
                recordingTargets.computeIfAbsent(classKey, ignored -> new LinkedHashSet<>()).add(fqcn);
            }

            for (final Method method : testClass.getDeclaredMethods())
            {
                final AiPlaybook methodAnnotation = method.getAnnotation(AiPlaybook.class);
                if (methodAnnotation != null)
                {
                    hasPlaybookAnnotation = true;
                    final String effectiveDir = firstNonBlank(methodAnnotation.recordingDirectory(), classDir, "<default>");
                    final String effectiveFile = firstNonBlank(methodAnnotation.recordingFileName(), classFile, testClass.getSimpleName());
                    final String methodKey = effectiveDir + "::" + effectiveFile;
                    recordingTargets.computeIfAbsent(methodKey, ignored -> new LinkedHashSet<>()).add(fqcn);
                }
            }

            if (hasPlaybookAnnotation)
            {
                scannedPlaybookClasses++;
            }
        }

        Assertions.assertTrue(scannedPlaybookClasses > 0, "Expected at least one @AiPlaybook test class");

        final List<String> collisions = new ArrayList<>();
        for (final Map.Entry<String, Set<String>> entry : recordingTargets.entrySet())
        {
            if (entry.getValue().size() > 1)
            {
                collisions.add(entry.getKey() + " -> " + entry.getValue());
            }
        }

        Assertions.assertTrue(collisions.isEmpty(),
            "Found parallel @AiPlaybook recording target collisions:\n" + String.join("\n", collisions));
    }

    private static Path resolveTestSourceRoot()
    {
        final Path fromMonorepoRoot = Path.of("neodymium-e2e-tests/src/test/java");
        if (Files.isDirectory(fromMonorepoRoot))
        {
            return fromMonorepoRoot;
        }
        return Path.of("src/test/java");
    }

    private static String toFqcn(final Path root, final Path testFile)
    {
        final String relative = root.relativize(testFile).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
    }

    private static String normalizeBlank(final String value)
    {
        if (value == null || value.isBlank())
        {
            return null;
        }
        return value.trim();
    }

    private static String firstNonBlank(final String primary, final String secondary, final String fallback)
    {
        final String normalizedPrimary = normalizeBlank(primary);
        if (normalizedPrimary != null)
        {
            return normalizedPrimary;
        }
        final String normalizedSecondary = normalizeBlank(secondary);
        if (normalizedSecondary != null)
        {
            return normalizedSecondary;
        }
        return fallback;
    }
}
