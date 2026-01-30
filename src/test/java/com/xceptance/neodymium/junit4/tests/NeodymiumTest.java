package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.model.FrameworkMethod;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.testclasses.datautils.TestDataTests;
import com.xceptance.neodymium.util.Neodymium;

public abstract class NeodymiumTest {
	// holds files that will be deleted in @After method
	protected static List<File> tempFiles = new LinkedList<>();

	@AfterClass
	public static void cleanUp() {
		for (File tempFile : tempFiles) {
			deleteTempFile(tempFile);
		}
	}

	/**
	 * Checks if a backup file for the specified configuration file exists, if so,
	 * the configuration file is removes and the backup is renamed to the original
	 * name
	 * 
	 * @param configFileName the file name of the configuration file
	 * @throws IOException if there are issues with the file handling or file system
	 */
	protected static void restoreConfigProperties(String configFileName) throws IOException {
		File backupFile = new File("./config/" + configFileName + ".backup");

		if (backupFile.exists()) {
			File configFile = new File("./config/" + configFileName);

			if (configFile.exists()) {
				Files.delete(configFile.toPath());

			}

			backupFile.renameTo(new File("./config/" + configFileName));
		}
	}

	/**
	 * Creates a copy of the specified configuration file with a ".backup" prefix
	 * 
	 * @param configFileName the file name of the configuration file
	 * @throws IOException if there are issues with the file handling or file system
	 */
	protected static void backUpConfigProperties(String configFileName) throws IOException {
		File configFile = new File("./config/" + configFileName);
		File backupFile = new File("./config/" + configFileName + ".backup");

		if (configFile.exists() && backupFile.exists() == false) {
			Path targetPath = backupFile.toPath();
			Path originalPath = configFile.toPath();

			Files.copy(originalPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * delete a temporary test file
	 * 
	 * @param tempFile the tempFile that should be deleted after test execution
	 */
	public static void deleteTempFile(final File tempFile) {
		if (tempFile.exists()) {
			try {
				Files.delete(tempFile.toPath());
			} catch (Exception e) {
				System.out.println(MessageFormat.format("Couldn''t delete temporary file: ''{0}'' caused by {1}",
						tempFile.getAbsolutePath(), e));
			}
		}
	}

	/**
	 * Add some properties to be used in the current test, using a temporary file
	 * name.
	 * 
	 * @param fileName   the filename for the temporary file used to transport the
	 *                   properties to the test, <b>use unique filename for the test
	 *                   using it</b>
	 * @param properties a HashMap containing all the needed properties for this
	 *                   test case
	 */
	protected void addPropertiesForTest(String fileName, Map<String, String> properties) {
		// due to different states the configuration is in during initialization, we
		// need to add it to the properties as
		// well as the file

		// during the general initialization we need the Neodymium.configuration()
		for (String key : properties.keySet()) {
			Neodymium.configuration().setProperty(key, properties.get(key));
		}

		// the Neodymium.configuration() will be overwritten at one stage of the init
		// process, so we need to have the
		// config values in temporary files as well
		String fileLocation = "config/" + fileName;
		File tempConfigFile = new File("./" + fileLocation);
		NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
		ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

		tempFiles.add(tempConfigFile);
	}

	/**
	 * Basic method to perform assertions on a given test result.
	 * 
	 * @param result                  test result to validate
	 * @param expectSuccessful        the test result should be successful
	 * @param expectedRunCount        expected number of run tests (including
	 *                                ignored)
	 * @param expectedIgnoreCount     expected number of ignored tests
	 * @param expectedFailCount       expected number of failed tests
	 * @param expectedFailureMessages expected failure messages mapped by name of
	 *                                test method
	 */
	public void check(final Result result, final boolean expectSuccessful, final int expectedRunCount,
			final int expectedIgnoreCount, final int expectedFailCount,
			final Map<String, String> expectedFailureMessages) {
		final Optional<String> accumulatedTrace = result.getFailures().stream().map(Failure::getTrace)
				.reduce(String::concat);
		final String stackTrace = accumulatedTrace.orElse("n/a");
		try {
			Assert.assertEquals("Test successful", expectSuccessful, result.wasSuccessful());
			Assert.assertEquals("Method run count", expectedRunCount, result.getRunCount());
			Assert.assertEquals("Method ignore count", expectedIgnoreCount, result.getIgnoreCount());
			Assert.assertEquals("Method fail count", expectedFailCount, result.getFailureCount());

			if (expectedFailureMessages != null) {
				final int failureCount = result.getFailureCount();
				for (int i = 0; i < failureCount; i++) {
					final String methodName = result.getFailures().get(i).getDescription().getMethodName();
					Assert.assertEquals("Failure message", expectedFailureMessages.get(methodName),
							result.getFailures().get(i).getMessage());
				}
			}
		} catch (AssertionError e) {
			Assert.fail("Assertion failed. " + e.getMessage() + " Stack trace: " + stackTrace);
		}
	}

	public void checkAssumptionFailure(final Result result, final boolean expectSuccessful, final int expectedRunCount,
			final int expectedIgnoreCount, final int expectedFailCount, final int assumtionFailureCount,
			final Map<String, String> expectedFailureMessages) {
		check(result, expectSuccessful, expectedRunCount, expectedIgnoreCount, expectedFailCount,
				expectedFailureMessages);
		Assert.assertEquals("Method assumption failure count", assumtionFailureCount,
				result.getAssumptionFailureCount());
	}

	/**
	 * Assert that all tests have passed.
	 * 
	 * @param result              test result to validate
	 * @param expectedRunCount    expected number of run tests (including ignored)
	 * @param expectedIgnoreCount expected number of ignored tests
	 */
	public void checkPass(final Result result, final int expectedRunCount, final int expectedIgnoreCount) {
		check(result, true, expectedRunCount, expectedIgnoreCount, 0, null);
	}

	/**
	 * Assert that at least one test has failed.
	 * 
	 * @param result              test result to validate
	 * @param expectedRunCount    expected number of run tests (including ignored)
	 * @param expectedIgnoreCount expected number of ignored tests
	 * @param expectedFailCount   expected number of failed tests
	 */
	public void checkFail(final Result result, final int expectedRunCount, final int expectedIgnoreCount,
			final int expectedFailCount) {
		check(result, false, expectedRunCount, expectedIgnoreCount, expectedFailCount, null);
	}

	/**
	 * Assert that at least one test has failed.
	 * 
	 * @param result                 test result to validate
	 * @param expectedRunCount       expected number of run tests (including
	 *                               ignored)
	 * @param expectedIgnoreCount    expected number of ignored tests
	 * @param expectedFailCount      expected number of failed tests
	 * @param expectedFailureMessage expected message of all failures (same message
	 *                               for each failure)
	 */
	public void checkFail(final Result result, final int expectedRunCount, final int expectedIgnoreCount,
			final int expectedFailCount, final String expectedFailureMessage) {
		final HashMap<String, String> expectedFailureMessages = new HashMap<String, String>();
		for (Failure failure : result.getFailures()) {
			expectedFailureMessages.put(failure.getDescription().getMethodName(), expectedFailureMessage);
		}
		check(result, false, expectedRunCount, expectedIgnoreCount, expectedFailCount, expectedFailureMessages);
	}

	/**
	 * Assert that at least one test has failed.
	 * 
	 * @param result                  test result to validate
	 * @param expectedRunCount        expected number of run tests (including
	 *                                ignored)
	 * @param expectedIgnoreCount     expected number of ignored tests
	 * @param expectedFailCount       expected number of failed tests
	 * @param expectedFailureMessages expected failures messages mapped by name of
	 *                                test method
	 */
	public void checkFail(final Result result, final int expectedRunCount, final int expectedIgnoreCount,
			final int expectedFailCount, final Map<String, String> expectedFailureMessages) {
		check(result, false, expectedRunCount, expectedIgnoreCount, expectedFailCount, expectedFailureMessages);
	}

	/**
	 * Assert that the test description is valid.
	 * 
	 * @param testDescription         the test description that should be tested
	 * @param expectedTestDescription expected test description as String array
	 */
	public void checkDescription(final Description testDescription, final String[] expectedTestDescription) {
		final ArrayList<Description> testChildren = testDescription.getChildren();
		final String[] actualDescription = new String[testChildren.size()];

		for (int i = 0; i < testChildren.size(); i++) {
			actualDescription[i] = testChildren.get(i).getMethodName();
		}
		Arrays.sort(actualDescription);
		Arrays.sort(expectedTestDescription);

		Assert.assertArrayEquals(expectedTestDescription, actualDescription);
	}

	/**
	 * Assert that the test description is valid.
	 * 
	 * @param clazz                   the class whose description should be tested
	 * @param expectedTestDescription expected test description as String array
	 */
	public void checkDescription(final Class<?> clazz, final String[] expectedTestDescription) throws Throwable {
		checkDescription(new NeodymiumRunner(clazz).getDescription(), expectedTestDescription);
	}

	/**
	 * Assert that the test method annotations are valid.
	 * 
	 * @param clazz               the class whose methods annotations should be
	 *                            tested
	 * @param expectedAnnotations expected test methods annotations as Map of method
	 *                            names as key and List of String containing the
	 *                            toString output of the annotations
	 */
	public void checkAnnotations(final Class<?> clazz, final Map<String, List<String>> expectedAnnotations)
			throws Throwable {
		final Map<FrameworkMethod, Description> compDescriptions = new NeodymiumRunner(clazz).getChildDescriptions();
		boolean matching = true;

		String missing = "";
		for (Entry<String, List<String>> entry : expectedAnnotations.entrySet()) {
			String methodName = entry.getKey();
			List<String> expAnnotations = entry.getValue();

			final List<String> compAnnotations = getAnnotationsForMethod(compDescriptions, methodName);
			for (String expAnnotation : expAnnotations) {
				boolean expAnnotationFound = false;
				for (String compAnnotation : compAnnotations) {
					if (compAnnotation.matches(".*" + expAnnotation + ".*")) {
						expAnnotationFound = true;
						break;
					}
				}
				if (!expAnnotationFound) {
					missing += expAnnotation + ";";
				}
				matching &= expAnnotationFound;
			}
		}
		Assert.assertTrue("Not all annotations were found, missing annotations were: " + missing, matching);
	}

	private List<String> getAnnotationsForMethod(final Map<FrameworkMethod, Description> compDescriptions,
			final String methodName) {
		List<String> resultList = new ArrayList<String>();
		for (Entry<FrameworkMethod, Description> entry : compDescriptions.entrySet()) {
			Description description = entry.getValue();
			String descMethodName = description.getMethodName();
			if (descMethodName.equals(methodName)) {
				for (Annotation annotation : description.getAnnotations()) {
					resultList.add(annotation.toString());
				}
				break;
			}
		}
		return resultList;
	}

	/**
	 * Helper method to write a {@link Map} to an file
	 * 
	 * @param map
	 * @param file
	 */
	public static void writeMapToPropertiesFile(final Map<String, String> map, final File file) {
		String propertiesString = map.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining(System.lineSeparator()));

		try {
			FileUtils.writeStringToFile(file, propertiesString, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Result run(Class<?> testClass) {
		Result result = JUnitCore.runClasses(testClass);
		for (int i = 0; i < 3 && result != null && result.getFailures() != null && !result.getFailures().isEmpty()
				&& result.getFailures().stream()
						.anyMatch(e -> e.getException() != null && e.getException().getMessage() != null
								&& e.getException().getMessage().contains("Could not start a new session")); i++) {
			result = JUnitCore.runClasses(testClass);
		}
		return result;
	}
}
