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
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.common.testdata.DataFile;

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
        // no specific file -> try the usual suspects
        final Set<String> fileNames = new LinkedHashSet<String>();

        DataFile dataFile = testClass.getAnnotation(DataFile.class);
        String filePath = dataFile != null ? dataFile.value() : null;

        if (StringUtils.isBlank(filePath))
        {
            final String dottedName = testClass.getName();
            final String slashedName = dottedName.replace('.', '/');

            String[] filetype = new String[]
            {
              ".csv", ".json", ".xml"
            };
            for (final String fileExtension : filetype)
            {
                fileNames.add(slashedName + fileExtension);
                fileNames.add(dottedName + fileExtension);
            }
        }
        else
        {
            InputStream inputStream = testClass.getResourceAsStream("/" + filePath);
            if (inputStream != null)
            {
                fileNames.add(filePath);
            }
            else
            {
                throw new RuntimeException("The data file:\"" + filePath + "\" provided within the test class:\"" + testClass.getSimpleName()
                                           + "\" can't be read.");
            }
        }

        List<File> dataSetFileDirs = new LinkedList<>();
        dataSetFileDirs.add(new File("."));

        // look for such a file in the usual directories
        return getDataSets(dataSetFileDirs, fileNames, testClass);
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
                    return readDataSetsFromFile(batchDataFile);
                }
            }
        }

        // look for a data set file in the class path
        for (final String fileName : fileNames)
        {
            final InputStream input = testClass.getResourceAsStream("/" + fileName);
            if (input != null)
            {
                OutputStream output = null;
                File batchDataFile = null;

                try
                {
                    // copy the stream to a temporary file
                    final String extension = "." + FilenameUtils.getExtension(fileName);
                    batchDataFile = File.createTempFile("dataSets_", extension);
                    output = new FileOutputStream(batchDataFile);

                    IOUtils.copy(input, output);
                    output.flush();

                    // read the data sets from the temporary file
                    return readDataSetsFromFile(batchDataFile);
                }
                finally
                {
                    // clean up
                    input.close();
                    output.close();
                    FileUtils.deleteQuietly(batchDataFile);
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
