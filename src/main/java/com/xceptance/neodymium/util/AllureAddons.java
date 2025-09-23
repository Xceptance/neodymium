package com.xceptance.neodymium.util;

import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableMap;
import com.xceptance.neodymium.common.ScreenshotWriter;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.internal.AllureStorage;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.xceptance.neodymium.util.PropertiesUtil.getPropertiesMapForCustomIdentifier;

/**
 * Convenience methods for step definitions
 *
 * @author rschwietzke
 */
public class AllureAddons
{
    private static final Properties ALLURE_PROPERTIES = io.qameta.allure.util.PropertiesUtils.loadAllureProperties();

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureAddons.class);

    private static boolean neoVersionLogged = false;

    static boolean customDataAdded = false;

    private static final int MAX_RETRY_COUNT = 10;

    // The local path where we want to save the script.
    // This will save it in a 'js' subdirectory in the report directory.
    public static final String JSON_VIEWER_SCRIPT_PATH = "js/json-viewer.min.js";

    private static boolean scriptDownloaded = false;

    /**
     * Define a step without return value. This can be used to transport data (information) from test into the report.
     *
     * @param info
     *     the info of the information (maybe the information itself if short enough), used in the description of this step
     * @param content
     *     further information that need to be passed to the report
     */
    @Step("INFO: {info}")
    public static void addToReport(String info, Object content)
    {
    }

    /**
     * Define a step without return value. This can be used to transport a simple message from test into the report.
     *
     * @param message
     *     the message to print directly into the report
     */
    @Step("{message}")
    public static void printToReport(String message)
    {
    }

    /**
     * Define a step without return value. This is good for complete and encapsulated test steps.
     *
     * @param description
     *     the proper description of this step
     * @param actions
     *     what to do as Lambda
     * @throws IOException
     */
    @Step("{description}")
    public static void step(final String description, final Runnable actions) throws IOException
    {
        try
        {
            actions.run();
        }
        finally
        {
            if (Neodymium.configuration().screenshotPerStep())
            {
                attachPNG(UUID.randomUUID().toString() + ".png");
            }
        }
    }

    /**
     * Define a step with a return value. This is good for complete and encapsulated test steps.
     *
     * @param <T>
     *     generic return type
     * @param description
     *     the proper description of this step
     * @param actions
     *     what to do as Lambda
     * @return T
     * @throws IOException
     */
    @Step("{description}")
    public static <T> T step(final String description, final Supplier<T> actions) throws IOException
    {
        try
        {
            return actions.get();
        }
        finally
        {
            if (Neodymium.configuration().screenshotPerStep())
            {
                attachPNG(UUID.randomUUID().toString() + ".png");
            }
        }
    }

    /**
     * Takes screenshot and converts it to byte stream
     *
     * @param filename
     * @throws IOException
     */
    public static void attachPNG(final String filename) throws IOException
    {
        // if we are running without a driver, we can't take a screenshot
        if (!Neodymium.hasDriver())
        {
            return;
        }

        // If there's a fullpage screenshot screenshot, we do both, if not we do not want to have two viewport
        // screenshots
        // if full page screenshot/advanced screenshotting is disabled we need the default
        if (Neodymium.configuration().enableViewportScreenshot() == true &&
            (Neodymium.configuration().enableAdvancedScreenShots() == false || Neodymium.configuration().enableFullPageCapture() == true))
        {
            // take a screenshot using the driver and write it to a file
            byte[] screenshot = ((TakesScreenshot) Neodymium.getDriver()).getScreenshotAs(OutputType.BYTES);
            FileUtils.writeByteArrayToFile(new File(filename), screenshot);
            // add to the allure report, no need put it into the correct step, since it will be there already during the
            // normal execution context.
            // Only on exception we don't know where to put it and that is handled elsewhere
            Allure.getLifecycle().addAttachment("Screenshot", "image/png", ".png", new FileInputStream(filename));

            new File(filename).delete();
        }
        if (Neodymium.configuration().enableAdvancedScreenShots() == true)
        {
            ScreenshotWriter.doScreenshot(filename);
        }
    }

    /**
     * Removes an already attached attachment from the allure report.
     *
     * @param name
     */
    public static void removeAttachmentFromStepByName(final String name)
    {

        AllureLifecycle lifecycle = Allure.getLifecycle();

        // suppress errors if we are running without allure
        if (canUpdateAllureTest())
        {
            lifecycle.updateTestCase((result) -> {
                StepResult stepResult = findLastStep(result.getSteps());
                List<Attachment> attachments = stepResult.getAttachments();
                for (int i = 0; i < attachments.size(); i++)
                {
                    io.qameta.allure.model.Attachment attachment = attachments.get(i);
                    if (attachment.getName().equals(name))
                    {
                        String path = ALLURE_PROPERTIES.getProperty("allure.results.directory", "allure-results");
                        // clean up from hard disk
                        File file = Paths.get(path).resolve(attachment.getSource()).toFile();
                        if (file.exists())
                        {
                            file.delete();
                        }
                        attachments.remove(i);
                        i--;
                    }
                }
            });
        }
    }

    /**
     * In before methods we will get a lot of error messages since internally Allure is has the current test not available.
     *
     * @return whether or not we can update the allure test case
     */
    public static boolean canUpdateAllureTest()
    {
        AllureLifecycle lifecycle = Allure.getLifecycle();

        try
        {
            if (lifecycle.getCurrentTestCase().isEmpty())
            {
                return false;
            }

            Field storageField = AllureLifecycle.class.getDeclaredField("storage");

            storageField.setAccessible(true);

            AllureStorage storage = (AllureStorage) storageField.get(lifecycle);

            if (storage.getTestResult(lifecycle.getCurrentTestCase().get()).isPresent()) // FIXME: is this not working
            // correctly with screens on
            // every step???
            {
                // now let's check if there are any steps ins
                AtomicBoolean hasSteps = new AtomicBoolean(false);
                lifecycle.updateTestCase((result) -> {
                    hasSteps.set(result.getSteps().isEmpty() == false);
                });
                return hasSteps.get();
            }
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return false;
    }

    /***
     * Add an Allure attachment to the current step instead of to the overall test case.
     *
     * @param name
     *            the name of attachment
     * @param type
     *            the content type of attachment
     * @param fileExtension
     *            the attachment file extension
     * @param stream
     *            attachment content
     * @return
     */
    public static boolean addAttachmentToStep(final String name, final String type,
                                              final String fileExtension, final InputStream stream)
    {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        // suppress errors if we are running without allure
        if (canUpdateAllureTest())
        {
            lifecycle.addAttachment(name, type, fileExtension, stream);

            lifecycle.updateTestCase((result) -> {
                StepResult stepResult = findLastStep(result.getSteps());
                Optional<io.qameta.allure.model.Attachment> addedAttachmentInOuterStep = result.getAttachments().stream().filter(a -> a.getName().equals(name))
                                                                                               .findFirst();

                boolean isAttachmentInCurrentStep = stepResult.getAttachments().stream().anyMatch(a -> a.getName().equals(name));
                if (!isAttachmentInCurrentStep && addedAttachmentInOuterStep.isPresent())
                {
                    stepResult.getAttachments().add(addedAttachmentInOuterStep.get());
                }

                addedAttachmentInOuterStep.ifPresent(attachment -> result.getAttachments().remove(attachment));
            });
            return true;
        }
        return false;
    }

    /**
     * Adds a step with the given information before the current step
     *
     * @param info
     *     message to be displayed before the step
     */
    public static void addInfoBeforeStep(final String info)
    {
        AllureLifecycle lifecycle = Allure.getLifecycle();

        if (canUpdateAllureTest())
        {

            lifecycle.updateTestCase((testResult -> {
                int position = testResult.getSteps().isEmpty() ? 0 : testResult.getSteps().size() - 1;

                testResult.getSteps().add(position, new StepResult()
                    .setName(info)
                    .setStart(System.currentTimeMillis())
                    .setStatus(io.qameta.allure.model.Status.PASSED)
                    .setStatusDetails(new io.qameta.allure.model.StatusDetails()));
            }));
        }
    }

    /**
     * Adds a step with the given information as the first step of the test case.
     *
     * @param info
     *     message to be displayed as the first step
     */
    public static void addInfoAsFirstStep(final String info)
    {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        if (canUpdateAllureTest())
        {

            lifecycle.updateTestCase((testResult -> {
                testResult.getSteps().add(0, new StepResult()
                    .setName(info)
                    .setStart(System.currentTimeMillis())
                    .setStatus(io.qameta.allure.model.Status.PASSED)
                    .setStatusDetails(new io.qameta.allure.model.StatusDetails()));
            }));
        }
    }

    /**
     * Finds the last active step of a list of steps.
     *
     * @param steps
     * @return
     */
    private static StepResult findLastStep(List<StepResult> steps)
    {
        StepResult lastStep = steps.get(steps.size() - 1);
        List<StepResult> childStepts = lastStep.getSteps();
        if (childStepts != null && childStepts.isEmpty() == false)
        {
            return findLastStep(childStepts);
        }
        return lastStep;
    }

    public static enum EnvironmentInfoMode
    {
        REPLACE, APPEND_VALUE, ADD, IGNORE;
    }

    /**
     * Adds information about environment to the report, if a key is already present in the map the current value will be kept
     *
     * @param environmentValuesSet
     *     map with environment values
     */
    public static synchronized void addEnvironmentInformation(ImmutableMap<String, String> environmentValuesSet)
    {
        addEnvironmentInformation(environmentValuesSet, EnvironmentInfoMode.REPLACE);
    }

    /**
     * Adds information about environment to the report
     *
     * @param environmentValuesSet
     *     map with environment values
     * @param mode
     *     if a key is already present in the map, should we replace the it with the new value, or should we add another line with the same key but different
     *     values or append the new value to the old value
     */
    public static synchronized void addEnvironmentInformation(ImmutableMap<String, String> environmentValuesSet, EnvironmentInfoMode mode)
    {
        try
        {
            FileLock lock = null;
            int retries = 0;
            do
            {
                if (retries > 0)
                {
                    Selenide.sleep(100);
                }
                try
                {
                    lock = FileChannel.open(Paths.get(getEnvFile().getAbsolutePath()), StandardOpenOption.APPEND).tryLock();
                }
                catch (OverlappingFileLockException e)
                {
                    LOGGER.debug(getEnvFile() + " is already locked");
                }
                retries++;
            }
            while (retries < MAX_RETRY_COUNT && lock == null);
            if (lock != null)
            {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();

                // check if allure-results folder exists
                if (!getAllureResultsFolder().exists())
                {
                    // create it if not
                    getAllureResultsFolder().mkdirs();
                }
                Document doc;
                boolean isFileAccessNeeded = false;

                // if environment.xml file exists, there probably already was an entry in it
                // in this case we need to append our values to it
                if (getEnvFile().length() != 0)
                {
                    doc = docBuilder.parse(getEnvFile());
                    for (Map.Entry<String, String> entry : environmentValuesSet.entrySet())
                    {
                        Node environment = doc.getDocumentElement();
                        NodeList childNodes = environment.getChildNodes();

                        boolean isSameNode = false;
                        int keyToUpdate = -1;
                        String value = "";
                        for (int i = 0; i < childNodes.getLength(); i++)
                        {
                            Node child = childNodes.item(i);
                            NodeList subNodes = child.getChildNodes();
                            String key = "";
                            for (int j = 0; j < subNodes.getLength(); j++)
                            {
                                Node subNode = subNodes.item(j);
                                if ("key".equals(subNode.getNodeName()))
                                {
                                    key = subNode.getTextContent();
                                }
                                else if ("value".equals(subNode.getNodeName()))
                                {
                                    value = subNode.getTextContent();
                                }
                            }
                            if (key.equals(entry.getKey()) && value.equals(entry.getValue()))
                            {
                                isSameNode = true;
                                break;
                            }
                            else if (key.equals(entry.getKey()))
                            {
                                keyToUpdate = i;
                                break;
                            }
                        }
                        if (!isSameNode)
                        {
                            // if we have the same key, we need to process it according to the chosen mode
                            if (keyToUpdate >= 0)
                            {
                                switch (mode)
                                {
                                    case REPLACE:
                                    {
                                        Element parameter = doc.createElement("parameter");
                                        Element keyNode = doc.createElement("key");
                                        Element valueNode = doc.createElement("value");
                                        keyNode.appendChild(doc.createTextNode(entry.getKey()));
                                        valueNode.appendChild(doc.createTextNode(entry.getValue()));

                                        parameter.appendChild(keyNode);
                                        parameter.appendChild(valueNode);
                                        environment.replaceChild(parameter, childNodes.item(keyToUpdate));
                                        isFileAccessNeeded = true;

                                        break;
                                    }
                                    case APPEND_VALUE:
                                    {
                                        if (value.contains(entry.getValue()) == false)
                                        {
                                            Element parameter = doc.createElement("parameter");
                                            Element keyNode = doc.createElement("key");
                                            Element valueNode = doc.createElement("value");
                                            keyNode.appendChild(doc.createTextNode(entry.getKey()));
                                            // append string as comma seperated list
                                            valueNode.appendChild(doc.createTextNode(value + ", " + entry.getValue()));

                                            parameter.appendChild(keyNode);
                                            parameter.appendChild(valueNode);
                                            environment.replaceChild(parameter, childNodes.item(keyToUpdate));
                                            isFileAccessNeeded = true;
                                        }
                                        break;
                                    }
                                    case ADD:
                                    {
                                        Element parameter = doc.createElement("parameter");
                                        Element keyNode = doc.createElement("key");
                                        Element valueNode = doc.createElement("value");
                                        keyNode.appendChild(doc.createTextNode(entry.getKey()));
                                        valueNode.appendChild(doc.createTextNode(entry.getValue()));
                                        parameter.appendChild(keyNode);
                                        parameter.appendChild(valueNode);
                                        environment.appendChild(parameter);
                                        isFileAccessNeeded = true;

                                        break;
                                    }
                                    case IGNORE:
                                        // IGNORE is... well ignore
                                        break;
                                }
                            }
                            else
                            {
                                // if there's no key duplication we will just add the new node
                                Element parameter = doc.createElement("parameter");
                                Element keyNode = doc.createElement("key");
                                Element valueNode = doc.createElement("value");
                                keyNode.appendChild(doc.createTextNode(entry.getKey()));
                                valueNode.appendChild(doc.createTextNode(entry.getValue()));
                                parameter.appendChild(keyNode);
                                parameter.appendChild(valueNode);
                                environment.appendChild(parameter);
                                isFileAccessNeeded = true;

                            }
                        }
                    }
                }
                else
                {
                    isFileAccessNeeded = true;
                    doc = docBuilder.newDocument();
                    Element environment = doc.createElement("environment");
                    doc.appendChild(environment);
                    for (Map.Entry<String, String> entry : environmentValuesSet.entrySet())
                    {
                        Element parameter = doc.createElement("parameter");
                        Element key = doc.createElement("key");
                        Element value = doc.createElement("value");
                        key.appendChild(doc.createTextNode(entry.getKey()));
                        value.appendChild(doc.createTextNode(entry.getValue()));
                        parameter.appendChild(key);
                        parameter.appendChild(value);
                        environment.appendChild(parameter);
                    }
                }
                if (isFileAccessNeeded)
                {
                    DOMSource source = new DOMSource(doc);
                    try (FileOutputStream output = new FileOutputStream(getEnvFile()))
                    {
                        StreamResult result = new StreamResult(output);
                        transformer.transform(source, result);
                    }
                }
                lock.release();
            }
            else
            {
                LOGGER.warn("Could not acquire Filelock in time. Failed to add information about enviroment to Allure report");
            }
        }
        catch (ParserConfigurationException | TransformerException | SAXException |

            IOException e)
        {
            LOGGER.warn("Failed to add information about environment to Allure report", e);
        }
    }

    /**
     * Check if allure-reprot environment.xml file exists
     *
     * @return false - if doesn't exist <br> true - if exists
     */
    public static boolean envFileExists()
    {
        return getEnvFile().exists();
    }

    private static File getEnvFile()
    {
        File allureResultsDir = getAllureResultsFolder();
        File envFile = new File(allureResultsDir.getAbsoluteFile() + File.separator + "environment.xml");
        if (!envFile.exists())
        {
            try
            {
                envFile.getParentFile().mkdirs();
                envFile.createNewFile();
            }
            catch (IOException e)
            {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return envFile;
    }

    /**
     * Get path to allure-results folder (default or configured in pom)
     *
     * @return File with path to the allure-results folder
     */
    public static File getAllureResultsFolder()
    {
        return new File(System.getProperty("allure.results.directory", System.getProperty("user.dir")
            + File.separator + "target" + File.separator + "allure-results"));
    }

    /**
     * Add a step to the report which contains a clickable url
     *
     * @param message
     *     message to be displayed before link
     * @param url
     *     url for the link
     */
    @Step("{message}: {url}")
    public static void addLinkToReport(String message, String url)
    {
    }

    public static void initializeEnvironmentInformation()
    {
        Map<String, String> environmentDataMap = new HashMap<String, String>();
        String customDataIdentifier = "neodymium.report.environment.custom";

        if (!neoVersionLogged && Neodymium.configuration().logNeoVersion())
        {
            LOGGER.info("This test uses Neodymium Library (version: " + Neodymium.getNeodymiumVersion()
                            + "), MIT License, more details on https://github.com/Xceptance/neodymium");
            neoVersionLogged = true;
            environmentDataMap.putIfAbsent("Testing Framework", "Neodymium " + Neodymium.getNeodymiumVersion());
        }
        if (!customDataAdded && Neodymium.configuration().enableCustomEnvironmentData())
        {
            LOGGER.info("Custom Environment Data was added.");
            customDataAdded = true;

            environmentDataMap.putAll(getPropertiesMapForCustomIdentifier(customDataIdentifier));
        }

        if (!environmentDataMap.isEmpty())
        {
            // These values should be the same for all running JVMs. If there are differences in the values, it would we
            // good to see it in the report
            // AllureAddons.addEnvironmentInformation(ImmutableMap.<String, String>
            // builder().putAll(environmentDataMap).build(), EnvironmentInfoMode.ADD);
            AllureAddons.addEnvironmentInformation(
                ImmutableMap.<String, String> builder().putAll(removePrefixFromMap(environmentDataMap, customDataIdentifier))
                            .build(),
                EnvironmentInfoMode.ADD);
        }
    }

    /**
     * Removes the prefix from the keys in the map.
     *
     * @param map
     *     the map to process
     * @param prefix
     *     the prefix to remove
     * @return a new map with the prefix removed from the keys
     */
    private static Map<String, String> removePrefixFromMap(Map<String, String> map, String prefix)
    {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            if (entry.getKey().startsWith(prefix))
            {
                result.put(entry.getKey().substring(prefix.length() + 1), entry.getValue());
            }
            else
            {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * @param name
     *     of the attachment
     * @param data
     *     that needs to be added as an attachment
     */
    public static void addDataAsJsonToReport(String name, Object data)
    {
        ObjectMapper mapper = new ObjectMapper();

        // In case we have a long which is out side the value range of JS' Number, we need to have some special
        // treatment
        SimpleModule module = new SimpleModule();
        JsonSerializer<Long> longSerializer = new JsonSerializer<Long>()
        {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException
            {
                convertValuesOutsideRangeToString(value, gen);
            }

            private void convertValuesOutsideRangeToString(Long value, JsonGenerator gen) throws IOException
            {
                if (value >= 9007199254740991l ||
                    value <= -9007199254740991l)
                {
                    gen.writeString(value.toString() + " (Longs outside JS Number limits are shown as strings)");
                }
                else
                {
                    gen.writeNumber(value);
                }
            }
        };

        module.addSerializer(Long.class, longSerializer);
        module.addSerializer(Long.TYPE, longSerializer);

        mapper.registerModule(module);

        String dataObjectJson;

        try
        {
            // covert Java object to JSON strings
            dataObjectJson = mapper.setSerializationInclusion(Include.NON_NULL).writeValueAsString(data);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }

        Allure.addAttachment(name, "text/html", DataUtils.convertJsonToHtml(dataObjectJson), "html");
    }

    public static synchronized void downloadJsonViewerScript()
    {
        if (scriptDownloaded)
        {
            return;
        }

        String scriptUrl = "https://cdn.jsdelivr.net/npm/@textea/json-viewer@3";

        int retry = 1;
        while (!scriptDownloaded && retry < 4)
        {
            try
            {
                LOGGER.info("Downloading JSON viewer script attempt: {}", retry);
                LOGGER.info("Starting download from: {}", scriptUrl);
                downloadFileFromUrl(scriptUrl, JSON_VIEWER_SCRIPT_PATH);
                LOGGER.info("Download complete! Script saved to: {}", JSON_VIEWER_SCRIPT_PATH);
                scriptDownloaded = true;
            }
            catch (Exception e)
            {
                LOGGER.error("An error occurred during download: {}", e.getMessage());
                LOGGER.error(e.getMessage(), e);
            }
            finally
            {
                retry++;
            }
        }

        if (retry >= 4)
        {
            LOGGER.info("Max number of retries reached");
        }
    }

    /**
     * Downloads a file from a given URL and saves it to a local destination path.
     *
     * @param urlString
     *     The URL of the file to download.
     * @param destinationPath
     *     The local file path (including directory and filename) to save the file to.
     * @throws IOException
     *     If a network or file system error occurs.
     */
    static void downloadFileFromUrl(String urlString, String destinationPath) throws IOException
    {
        // Create a Path object from the destination string.
        Path destination = Paths.get(destinationPath);

        // If the file is found return
        if (Files.exists(destination))
        {
            LOGGER.info("File already exists. Skipping download.");
            return;
        }

        // --- Important: Ensure the parent directory exists ---
        // If the destination is "js/script.js", this will create the "js" directory
        // if it does not already exist. This prevents a common file system error.
        Path parentDir = destination.getParent();
        if (parentDir != null && !Files.exists(parentDir))
        {
            LOGGER.info("Creating directory: {}", parentDir);
            Files.createDirectories(parentDir);
        }

        // Create a URL object from the string.
        URL url = new URL(urlString);

        // Open a connection to the URL.
        URLConnection connection = url.openConnection();

        // Use a try-with-resources statement to automatically close the input stream.
        // This is a modern and safe way to handle I/O resources.
        try (InputStream in = connection.getInputStream())
        {
            // Copy the data from the input stream (the web) to the destination file.
            // StandardCopyOption.REPLACE_EXISTING ensures that if the file already
            // exists, it will be overwritten with the new version.
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
