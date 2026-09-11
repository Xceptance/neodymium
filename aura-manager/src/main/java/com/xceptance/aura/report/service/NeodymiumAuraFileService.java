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
package com.xceptance.aura.report.service;

import com.xceptance.aura.report.dto.TestClassInfoDto;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service managing native test data file browsing, reading, saving, and detecting Java test classes inside src/test/java.
 *
 * @author Xceptance GmbH 2026
 */
@Service
public class NeodymiumAuraFileService
{
    private static final Logger LOG = LoggerFactory.getLogger(NeodymiumAuraFileService.class);
    private static final String DATA_DIR = Paths.get("").toAbsolutePath().resolve("src/main/resources/com/xceptance/neodymium/aura/data").toString();

    public List<String> listTestDataFiles()
    {
        final List<String> fileList = new ArrayList<>();
        final File rootDir = new File(DATA_DIR);
        if (rootDir.exists() && rootDir.isDirectory())
        {
            scanDirectory(rootDir, fileList);
        }
        return fileList;
    }

    public List<TestClassInfoDto> detectTestClasses()
    {
        final List<TestClassInfoDto> results = new ArrayList<>();
        final List<File> testRoots = List.of(
            Paths.get("").toAbsolutePath().resolve("src/test/java").toFile(),
            new File("./src/test/java")
        );

        for (final File root : testRoots)
        {
            if (root.exists() && root.isDirectory())
            {
                scanJavaTestFiles(root, root, results);
            }
        }

        results.sort(Comparator.comparing(TestClassInfoDto::getSimpleName));
        return results;
    }

    public String readFileContent(final String relativePath)
    {
        final File target = resolvePath(relativePath);
        if (target != null && target.exists() && target.isFile())
        {
            try
            {
                return Files.readString(target.toPath(), StandardCharsets.UTF_8);
            }
            catch (final IOException e)
            {
                LOG.error("Failed to read test data file {}: {}", relativePath, e.getMessage());
            }
        }
        return "";
    }

    public boolean saveFileContent(final String relativePath, final String content)
    {
        final File target = resolvePath(relativePath);
        if (target != null)
        {
            try
            {
                if (!target.getParentFile().exists())
                {
                    target.getParentFile().mkdirs();
                }
                Files.writeString(target.toPath(), content != null ? content : "", StandardCharsets.UTF_8);
                LOG.info("Successfully updated test data file: {}", target.getAbsolutePath());
                return true;
            }
            catch (final IOException e)
            {
                LOG.error("Failed to save test data file {}: {}", relativePath, e.getMessage());
            }
        }
        return false;
    }

    private File resolvePath(final String relativePath)
    {
        if (relativePath == null || relativePath.isEmpty())
        {
            return null;
        }
        final String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return new File(DATA_DIR, cleanPath);
    }

    private void scanDirectory(final File dir, final List<String> results)
    {
        final File[] files = dir.listFiles();
        if (files == null) return;

        for (final File f : files)
        {
            if (f.isDirectory())
            {
                scanDirectory(f, results);
            }
            else if (f.getName().endsWith(".yaml") || f.getName().endsWith(".yml") || f.getName().endsWith(".json"))
            {
                final String rel = f.getAbsolutePath().substring(new File(DATA_DIR).getAbsolutePath().length());
                results.add(rel.startsWith("/") ? rel.substring(1) : rel);
            }
        }
    }

    private void scanJavaTestFiles(final File rootDir, final File currentDir, final List<TestClassInfoDto> results)
    {
        final File[] files = currentDir.listFiles();
        if (files == null) return;

        for (final File f : files)
        {
            if (f.isDirectory())
            {
                scanJavaTestFiles(rootDir, f, results);
            }
            else if (f.getName().endsWith(".java") && (f.getName().endsWith("Test.java") || f.getName().endsWith("Tests.java")))
            {
                final String relPath = f.getAbsolutePath().substring(rootDir.getAbsolutePath().length());
                final String cleanPath = relPath.startsWith("/") ? relPath.substring(1) : relPath;
                final String classPath = cleanPath.replaceAll("\\.java$", "").replace('/', '.');

                final String simpleName = f.getName().replaceAll("\\.java$", "");
                final int lastDot = classPath.lastIndexOf('.');
                final String pkgName = lastDot > 0 ? classPath.substring(0, lastDot) : "default";

                final String category = pkgName.contains("aura") ? "Aura Manager Tests" :
                                         pkgName.contains("junit5") ? "Neodymium Framework Tests" : "Integration Tests";

                results.add(new TestClassInfoDto(simpleName, classPath, pkgName, category));
            }
        }
    }
}
