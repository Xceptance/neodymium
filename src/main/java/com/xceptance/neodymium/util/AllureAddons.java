package com.xceptance.neodymium.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.model.StepResult;

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

    private static boolean customDataAdded = false;

    private static final int MAX_RETRY_COUNT = 10;

    /**
     * Define a step without return value. This can be used to transport data (information) from test into the report.
     *
     * @param info
     *            the info of the information (maybe the information itself if short enough), used in the description of
     *            this step
     * @param content
     *            further information that need to be passed to the report
     */
    @Step("INFO: {info}")
    public static void addToReport(String info, Object content)
    {
    }

    /**
     * Define a step without return value. This is good for complete and encapsulated test steps.
     *
     * @param description
     *            the proper description of this step
     * @param actions
     *            what to do as Lambda
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
     *            generic return type
     * @param description
     *            the proper description of this step
     * @param actions
     *            what to do as Lambda
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
    @Attachment(type = "image/png", value = "{filename}", fileExtension = ".png")
    public static void attachPNG(final String filename) throws IOException
    {
        if (Neodymium.configuration().enableAdvancedScreenShots() == false)
        {
            ((TakesScreenshot) Neodymium.getDriver()).getScreenshotAs(OutputType.BYTES);
        }
        else
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
        if (lifecycle.getCurrentTestCase().isPresent())
        {
            lifecycle.updateTestCase((result) -> {
                var stepResult = findCurrentStep(result.getSteps());
                var attachments = stepResult.getAttachments();
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
        if (lifecycle.getCurrentTestCase().isPresent())
        {
            lifecycle.addAttachment(name, type, fileExtension, stream);

            lifecycle.updateTestCase((result) -> {
                var stepResult = findCurrentStep(result.getSteps());
                var attachment = result.getAttachments().get(result.getAttachments().size() - 1);
                result.getAttachments().remove(result.getAttachments().size() - 1);
                stepResult.getAttachments().add(attachment);
            });
            return true;
        }
        return false;
    }

    /**
     * Finds the last active step of a list of steps.
     * 
     * @param steps
     * @return
     */
    private static StepResult findCurrentStep(List<StepResult> steps)
    {
        var lastStep = steps.get(steps.size() - 1);
        List<StepResult> childStepts = lastStep.getSteps();
        if (childStepts != null && childStepts.isEmpty() == false)
        {
            return findCurrentStep(childStepts);
        }
        return lastStep;
    }

    public static enum EnvironmentInfoMode
    {
        REPLACE, APPEND_VALUE, ADD, IGNORE;
    }

    /**
     * Adds information about environment to the report, if a key is already present in the map the current value will
     * be kept
     * 
     * @param environmentValuesSet
     *            map with environment values
     */
    public static synchronized void addEnvironmentInformation(ImmutableMap<String, String> environmentValuesSet)
    {
        addEnvironmentInformation(environmentValuesSet, EnvironmentInfoMode.REPLACE);
    }

    /**
     * Adds information about environment to the report
     * 
     * @param environmentValuesSet
     *            map with environment values
     * @param mode
     *            if a key is already present in the map, should we replace the it with the new value, or should we add
     *            another line with the same key but different values or append the new value to the old value
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
     * @return false - if doesn't exist <br>
     *         true - if exists
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
     *            message to be displayed before link
     * @param url
     *            url for the link
     */
    @Step("{message}: {url}")
    public static void addLinkToReport(String message, String url)
    {
    }

    public static void initializeEnvironmentInformation()
    {
        Map<String, String> environmentDataMap = new HashMap<String, String>();

        if (!neoVersionLogged && Neodymium.configuration().logNeoVersion())
        {
            LOGGER.info("This test uses Neodymium Library (version: " + Neodymium.getNeodymiumVersion()
                        + "), MIT License, more details on https://github.com/Xceptance/neodymium-library");
            neoVersionLogged = true;
            environmentDataMap.putIfAbsent("Testing Framework", "Neodymium " + Neodymium.getNeodymiumVersion());
        }
        if (!customDataAdded && Neodymium.configuration().enableCustomEnvironmentData())
        {
            LOGGER.info("Custom Environment Data was added.");
            customDataAdded = true;
            String customDataIdentifier = "neodymium.report.environment.custom";
            environmentDataMap = PropertiesUtil.addMissingPropertiesFromFile("." + File.separator + "config" + File.separator + "dev-neodymium.properties",
                                                                             customDataIdentifier, environmentDataMap);

            Map<String, String> systemEnvMap = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : System.getenv().entrySet())
            {
                String key = entry.getKey();
                if (key.contains(customDataIdentifier))
                {
                    String cleanedKey = key.replace(customDataIdentifier, "");
                    cleanedKey = cleanedKey.replaceAll("\\.", "");
                    systemEnvMap.put(cleanedKey, entry.getValue());
                }
            }
            environmentDataMap = PropertiesUtil.mapPutAllIfAbsent(environmentDataMap, systemEnvMap);
            environmentDataMap = PropertiesUtil.mapPutAllIfAbsent(environmentDataMap,
                                                                  PropertiesUtil.getDataMapForIdentifier(customDataIdentifier,
                                                                                                         System.getProperties()));
            environmentDataMap = PropertiesUtil.addMissingPropertiesFromFile("." + File.separator + "config" + File.separator + "credentials.properties",
                                                                             customDataIdentifier, environmentDataMap);
            environmentDataMap = PropertiesUtil.addMissingPropertiesFromFile("." + File.separator + "config" + File.separator + "neodymium.properties",
                                                                             customDataIdentifier, environmentDataMap);
        }

        if (!environmentDataMap.isEmpty())
        {
            // These values should be the same for all running JVMs. If there are differences in the values, it would we
            // good to see it in the report
            AllureAddons.addEnvironmentInformation(ImmutableMap.<String, String> builder().putAll(environmentDataMap).build(), EnvironmentInfoMode.ADD);
        }
    }

    /**
     * @param name
     *            of the attachment
     * @param data
     *            that needs to be added as an attachment
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
}
