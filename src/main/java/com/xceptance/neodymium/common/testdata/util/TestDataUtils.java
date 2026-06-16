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
package com.xceptance.neodymium.common.testdata.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.URL;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.util.Neodymium;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/**
 * Utility class for test data handling.
 * 
 * @author Hartmut Arlt (Xceptance Software Technologies GmbH)
 */
public final class TestDataUtils
{
    /**
     * Class logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataUtils.class);

    /**
     * Returns the test data sets associated with the given test case class.
     *
     * @param testClass
     *            the test case class
     * @return the data sets, or <code>null</code> if there are no associated test data sets
     * @throws FileNotFoundException
     *             if an explicitly configured data set file cannot be found
     * @throws IOException
     *             if an I/O error occurred
     */
    public static List<Map<String, String>> getDataSets(final Class<?> testClass) throws FileNotFoundException, IOException
    {
        Pattern fileFilterPattern = null;
        if (StringUtils.isNotBlank(Neodymium.configuration().getTestFileFilter())) {
            // Compile the filter regex once to optimize repeated matching during file discovery
            fileFilterPattern = Pattern.compile(Neodymium.configuration().getTestFileFilter());
        }

        DataFile[] dataFiles = testClass.getAnnotationsByType(DataFile.class);
        DataFolder[] dataFolders = testClass.getAnnotationsByType(DataFolder.class);

        // If this is an AI Test Generator, do not read data sets but automatically create data folders if requested
        boolean isGenerator = false;
        for (java.lang.annotation.Annotation a : testClass.getAnnotations()) {
            if (a.annotationType().getSimpleName().equals("NeodymiumTestGenerator")) {
                isGenerator = true;
                break;
            }
        }
        if (!isGenerator) {
            for (java.lang.reflect.Method m : testClass.getMethods()) {
                for (java.lang.annotation.Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getSimpleName().equals("NeodymiumTestGenerator")) {
                        isGenerator = true;
                        break;
                    }
                }
                if (isGenerator) break;
            }
        }
        if (isGenerator) {
            for (DataFolder dataFolder : dataFolders) {
                String folderPath = dataFolder.value();
                if (StringUtils.isNotBlank(folderPath)) {
                    new File("src/test/resources/" + folderPath).mkdirs();
                }
            }
            return new LinkedList<>();
        }

        List<Map<String, String>> resultDataSets = new LinkedList<>();
        Map<String, String> testIdToFileName = new HashMap<>();

        if (dataFiles.length == 0 && dataFolders.length == 0)
        {
            // no specific file -> try the usual suspects
            final Set<String> fileNames = new LinkedHashSet<String>();

            final String dottedName = testClass.getName();
            final String slashedName = dottedName.replace('.', '/');

            String[] filetype = new String[]
            {
              ".yaml", ".yml", ".json", ".csv", ".xml", ".properties"
            };
            for (final String fileExtension : filetype)
            {
                fileNames.add(slashedName + fileExtension);
                fileNames.add(dottedName + fileExtension);
            }

            if (fileFilterPattern != null) {
                final Pattern finalFilter = fileFilterPattern;
                // Pre-filter implicit files before triggering disk I/O
                fileNames.removeIf(name -> !finalFilter.matcher(name.replace('\\', '/')).find());
            }

            List<File> dataSetFileDirs = new LinkedList<>();
            dataSetFileDirs.add(new File("."));
            dataSetFileDirs.add(new File("src/test/resources"));

            List<Map<String, String>> fileDataSets = getDataSets(dataSetFileDirs, fileNames, testClass);
            
            appendDataSetsAndCheckIDs(resultDataSets, fileDataSets, dataSetFileDirs.get(0).getName(), testIdToFileName);
        }
        else
        {


        for (DataFile dataFile : dataFiles)
        {
            String filePath = dataFile.value();
            if (StringUtils.isBlank(filePath))
            {
                continue;
            }

            if (fileFilterPattern != null && !fileFilterPattern.matcher(filePath.replace('\\', '/')).find()) {
                // Skip reading this explicit @DataFile if it does not match the requested filter
                continue;
            }

            InputStream inputStream = testClass.getResourceAsStream("/" + filePath);
            File localFile = new File("src/test/resources/" + filePath);
            if (inputStream == null && !localFile.exists())
            {
                throw new RuntimeException("The data file:\"" + filePath + "\" provided within the test class:\"" + testClass.getSimpleName()
                                           + "\" can't be read.");
            }

            Set<String> fileNames = new LinkedHashSet<>();
            fileNames.add(filePath);

            List<File> dataSetFileDirs = new LinkedList<>();
            dataSetFileDirs.add(new File("."));
            dataSetFileDirs.add(new File("src/test/resources"));

            List<Map<String, String>> fileDataSets = getDataSets(dataSetFileDirs, fileNames, testClass);

            appendDataSetsAndCheckIDs(resultDataSets, fileDataSets, filePath, testIdToFileName);
        }

        for (DataFolder dataFolder : dataFolders)
        {
            String folderPath = dataFolder.value();
            if (StringUtils.isBlank(folderPath))
            {
                continue;
            }

            File folder = new File("src/test/resources/" + folderPath);
            if (!folder.exists() || !folder.isDirectory())
            {
                java.net.URL url = testClass.getResource("/" + folderPath);
                if (url != null && "file".equals(url.getProtocol()))
                {
                    try
                    {
                        folder = new File(url.toURI());
                    }
                    catch (Exception e)
                    {
                        LOGGER.warn("Failed to convert URI for folder: " + folderPath, e);
                    }
                }
            }

            if (!folder.exists()) {
                LOGGER.info("The data folder:\"" + folderPath + "\" provided within the test class:\"" + testClass.getSimpleName()
                                           + "\" does not exist. Creating it automatically.");
                new File("src/test/resources/" + folderPath).mkdirs();
                folder = new File("src/test/resources/" + folderPath);
            }

            if (folder.exists() && folder.isDirectory())
            {
                List<File> fileList = new LinkedList<>();
                collectFilesRecursively(folder, fileList);
                // Sort to ensure determinism
                fileList.sort((f1, f2) -> f1.getAbsolutePath().compareTo(f2.getAbsolutePath()));

                for (File file : fileList)
                {
                    if (file.isDirectory())
                    {
                        continue;
                    }

                    if (fileFilterPattern != null && !fileFilterPattern.matcher(file.getAbsolutePath().replace('\\', '/')).find()) {
                        // Skip parsing this discovered file entirely to save time during large test runs
                        continue;
                    }

                    if (!isSupportedExtension(file.getName()))
                    {
                        LOGGER.warn("Unsupported or ignored test data file: " + file.getAbsolutePath());
                        continue;
                    }

                    List<Map<String, String>> fileDataSets = readDataSetsFromFile(file);

                    appendDataSetsAndCheckIDs(resultDataSets, fileDataSets, file.getAbsolutePath(), testIdToFileName);
                }
            }
            else
            {
                LOGGER.warn("The data folder:\"" + folderPath + "\" provided within the test class:\"" + testClass.getSimpleName()
                                           + "\" does not exist or is not a directory.");
            }
        }

        }

        String targetTestId = Neodymium.configuration().getTestIdFilter();

        if (StringUtils.isNotBlank(targetTestId))
        {
            Pattern idFilter = Pattern.compile(targetTestId);

            // Filter parsed datasets. We must do this after file parsing because test IDs are defined inside the files.
            resultDataSets = resultDataSets.stream().filter(dataSet -> {
                String currentTestId = dataSet.get("testId");
                if (StringUtils.isBlank(currentTestId))
                {
                    currentTestId = dataSet.get("TEST_ID");
                }

                if (currentTestId == null) {
                    return false;
                }
                
                return idFilter.matcher(currentTestId).find();
            }).collect(Collectors.toList());
        }

        return resultDataSets;
    }

    private static void collectFilesRecursively(File folder, List<File> fileList)
    {
        File[] files = folder.listFiles();
        if (files == null) return;
        
        for (File f : files)
        {
            if (f.isDirectory())
            {
                collectFilesRecursively(f, fileList);
            }
            else
            {
                fileList.add(f);
            }
        }
    }

    private static boolean isSupportedExtension(String fileName)
    {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".csv") || lowerCaseName.endsWith(".xml") 
            || lowerCaseName.endsWith(".json") || lowerCaseName.endsWith(".yaml") 
            || lowerCaseName.endsWith(".yml") || lowerCaseName.endsWith(".properties");
    }

    private static void appendDataSetsAndCheckIDs(final List<Map<String, String>> result, final List<Map<String, String>> newDataSets, final String sourceName, final Map<String, String> testIdToFileName)
    {
        for (final Map<String, String> dataSet : newDataSets)
        {
            final String testIdVal = dataSet.get("testId");
            final String testId = StringUtils.isBlank(testIdVal) ? dataSet.get("TEST_ID") : testIdVal;

            final String sourceFileFromDataSet = dataSet.get("neodymium.sourceFile");
            final String actualSourceName = (StringUtils.isNotBlank(sourceFileFromDataSet) && !".".equals(sourceFileFromDataSet)) ? sourceFileFromDataSet : sourceName;

            if (StringUtils.isNotBlank(testId))
            {
                if (testIdToFileName.containsKey(testId))
                {
                    final String previousSourceName = testIdToFileName.get(testId);
                    if (!previousSourceName.equals(actualSourceName))
                    {
                        throw new RuntimeException("Duplicate test dataset ID '" + testId + "' found in file '" + actualSourceName + "'. Already defined in '" + previousSourceName + "'.");
                    }
                }
                testIdToFileName.put(testId, actualSourceName);
            }
            
            // Add internal metadata for tracking the source file
            dataSet.put("neodymium.sourceFile", actualSourceName);

            result.add(dataSet);
        }
    }

    /**
     * Looks for a data set file and, if found, returns its the data sets. Tries all the specified file names in all the
     * passed directories and finally in the class path.
     *
     * @param dataSetFileDirs
     *            the directories to search
     * @param fileNames
     *            the file names to try
     * @param testClass
     *            the test case class as the class path context
     * @return the data sets, or <code>null</code> if no data sets file was found
     * @throws IOException
     *             if an I/O error occurred
     */
    private static List<Map<String, String>> getDataSets(final List<File> dataSetFileDirs, final Set<String> fileNames,
                                                         final Class<?> testClass)
        throws IOException
    {
        // look for a data set file in the passed directories
        for (final File directory : dataSetFileDirs)
        {
            for (final String fileName : fileNames)
            {
                final File batchDataFile = new File(directory, fileName);
                if (batchDataFile.isFile())
                {
                    final List<Map<String, String>> datasets = readDataSetsFromFile(batchDataFile, null);
                    final String baseName = FilenameUtils.getName(fileName);
                    for (final Map<String, String> ds : datasets)
                    {
                        ds.put("neodymium.sourceFile", baseName);
                    }
                    return datasets;
                }
            }
        }

        // look for a data set file in the class path
        for (final String fileName : fileNames)
        {
            final URL url = testClass.getResource("/" + fileName);
            if (url != null)
            {
                if ("file".equals(url.getProtocol()))
                {
                    try
                    {
                        final File batchDataFile = new File(url.toURI());
                        if (batchDataFile.isFile())
                        {
                            final List<Map<String, String>> datasets = readDataSetsFromFile(batchDataFile, fileName);
                            final String baseName = FilenameUtils.getName(fileName);
                            for (final Map<String, String> ds : datasets)
                            {
                                ds.put("neodymium.sourceFile", baseName);
                            }
                            return datasets;
                        }
                    }
                    catch (final Exception e)
                    {
                        // Fall back to stream copying if URI conversion fails
                    }
                }

                OutputStream output = null;
                File batchDataFile = null;

                try (final InputStream input = url.openStream())
                {
                    // copy the stream to a temporary file
                    final String extension = "." + FilenameUtils.getExtension(fileName);
                    batchDataFile = File.createTempFile("dataSets_", extension);
                    output = new FileOutputStream(batchDataFile);

                    IOUtils.copy(input, output);
                    output.flush();

                    // read the data sets from the temporary file passing original classpath path context
                    final List<Map<String, String>> datasets = readDataSetsFromFile(batchDataFile, fileName);
                    final String baseName = FilenameUtils.getName(fileName);
                    for (final Map<String, String> ds : datasets)
                    {
                        ds.put("neodymium.sourceFile", baseName);
                    }
                    return datasets;
                }
                finally
                {
                    if (output != null)
                    {
                        output.close();
                    }
                    if (batchDataFile != null)
                    {
                        FileUtils.deleteQuietly(batchDataFile);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Returns the test data sets contained in the given test data file. The data set provider used to read the file is
     * determined from the data file's extension.
     *
     * @param dataSetsFile
     *            the test data set file
     * @return the data sets
     */
    public static List<Map<String, String>> readDataSetsFromFile(final File dataSetsFile)
    {
        return readDataSetsFromFile(dataSetsFile, null);
    }

    /**
     * Returns the test data sets contained in the given test data file, providing classpath resource context.
     *
     * @param dataSetsFile
     *            the test data set file
     * @param classpathResourcePath
     *            the original classpath resource path
     * @return the data sets
     */
    public static List<Map<String, String>> readDataSetsFromFile(final File dataSetsFile, final String classpathResourcePath)
    {
        LOGGER.debug("Test data set file used: " + dataSetsFile.getAbsolutePath());

        final String fileExtension = FilenameUtils.getExtension(dataSetsFile.getName());

        switch (fileExtension.toLowerCase())
        {
            case "csv":
                return CsvFileReader.readFile(dataSetsFile);

            case "xml":
                return XmlFileReader.readFile(dataSetsFile);

            case "json":
                return JsonFileReader.readFile(dataSetsFile);

            case "yaml":
            case "yml":
                return YamlFileReader.readFile(dataSetsFile, classpathResourcePath);

            case "properties":
                return PropertyFileReader.readFile(dataSetsFile);

            default:
                throw new NotImplementedException("Not implemented for file type: " + fileExtension);
        }
    }

    /**
     * Returns the package test data for the given test class.
     * 
     * @param clazz
     *            the test class
     * @return package test data
     */
    public static Map<String, String> getPackageTestData(final Class<?> clazz)
    {
        final Package pkg = clazz.getPackage();
        String packageName = ((pkg == null) ? "" : pkg.getName());

        try
        {
            String baseDir = ".";
            if (StringUtils.isNotBlank(packageName))
            {
                // use package structure to get locator to base directory
                // e.g. "com.foo.bar" will result in "../../.."
                String[] packages = packageName.split("\\.");
                Arrays.fill(packages, "..");
                baseDir = String.join("/", packages);
            }

            return getPackageTestData(clazz, baseDir, packageName);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to load test data for package '" + packageName + "'.", e);
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the package test data for the given script package.
     * 
     * @param clazz
     *            the context class to be used for resource lookup (pass {@code null} to force file lookup)
     * @param baseDir
     *            the base directory to be used for data file lookup
     * @param packageName
     *            the package name
     * @return the package test data
     */
    private static Map<String, String> getPackageTestData(final Class<?> clazz, final String baseDir, final String packageName)
    {
        final List<String> packages = new LinkedList<>();

        if (StringUtils.isNotBlank(packageName))
        {
            // create a list of packages to look up test data
            // com.foo.bar
            // com.foo
            // com
            List<String> packageParts = new LinkedList<>(Arrays.asList(packageName.split("\\.")));
            while (packageParts.size() > 0)
            {
                packages.add(String.join(".", packageParts));
                packageParts.remove(packageParts.size() - 1);
            }
            // reverse the package list so we will first look up "com" then "com.foo"
            Collections.reverse(packages);
        }

        // the final test data map
        final Map<String, String> m = new HashMap<String, String>();
        for (String pck : packages)
        {
            // add file contents if present
            Map<String, String> newData = readPackageTestData(clazz, baseDir, pck);
            // iterate over data entries to put them one by one in the map
            for (Entry<String, String> newDataEntry : newData.entrySet())
            {
                // log if it is a new entry or if it overwrites an existing one
                if (m.containsKey(newDataEntry.getKey()))
                {
                    LOGGER.debug(String.format("Data entry \"%s\" overwritten by test data from package \"%s\" (old: \"%s\", new: \"%s\")",
                                               newDataEntry.getKey(), pck, m.get(newDataEntry.getKey()), newDataEntry.getValue()));
                }
                else
                {
                    LOGGER.debug(String.format("New package test data entry \"%s\"=\"%s\" in package \"%s\"", newDataEntry.getKey(),
                                               newDataEntry.getValue(), pck));
                }
                m.put(newDataEntry.getKey(), newDataEntry.getValue());
            }
        }
        return m;
    }

    /**
     * Loads and returns the package test data for the given script package.
     *
     * @param clazz
     *            the class object to use for resource lookup
     * @param baseDir
     *            the base directory to use for data file lookup
     * @param packageName
     *            the name of the script package
     * @return test data of given script package
     */
    public static Map<String, String> readPackageTestData(final Class<?> clazz, final String baseDir, final String packageName)
    {
        final String baseName = packageName.replace('.', '/') + "/package_testdata.";
        final String base = baseDir + "/" + baseName;

        try
        {
            InputStream is = null;
            String path;

            path = base + "yaml";
            is = clazz.getResourceAsStream(path);
            if (is != null)
            {
                return getFirstDataSetFromFile(YamlFileReader.readFile(is), path);
            }

            path = base + "yml";
            is = clazz.getResourceAsStream(path);
            if (is != null)
            {
                return getFirstDataSetFromFile(YamlFileReader.readFile(is), path);
            }

            path = base + "csv";
            is = clazz.getResourceAsStream(path);
            if (is != null)
            {
                return getFirstDataSetFromFile(CsvFileReader.readFile(is), path);
            }

            path = base + "xml";
            is = clazz.getResourceAsStream(path);
            if (is != null)
            {
                return getFirstDataSetFromFile(XmlFileReader.readFile(is), path);
            }

            path = base + "json";
            is = clazz.getResourceAsStream(path);
            if (is != null)
            {
                return getFirstDataSetFromFile(JsonFileReader.readFile(is), path);
            }

            path = base + "properties";
            is = clazz.getResourceAsStream(path);
            if (is != null)
            {
                return getFirstDataSetFromFile(PropertyFileReader.readFile(is), path);
            }

            // TODO: discuss order of file extensions
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to parse package test data for package '" + packageName + "'", e);
        }

        return Collections.emptyMap();

    }

    private static Map<String, String> getFirstDataSetFromFile(List<Map<String, String>> list, String path)
    {
        if (list.size() == 0)
        {
            LOGGER.warn("No data set found in data file: " + path);
        }
        else if (list.size() > 1)
        {
            LOGGER.warn("More than one data set found in data file: " + path);
        }
        return list.get(0);
    }
}
