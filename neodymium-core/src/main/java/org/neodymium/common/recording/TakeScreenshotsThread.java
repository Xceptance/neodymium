package org.neodymium.common.recording;

import java.awt.image.BufferedImage;
import org.neodymium.common.recording.config.RecordingConfigurations;
import org.neodymium.common.recording.writers.Writer;
import org.neodymium.util.AllureAddons;
import io.qameta.allure.Allure;
import javax.imageio.ImageIO;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import org.neodymium.util.Neodymium;
import static org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent;

/**
 * Background thread to take screenshots and write them to the files using {@link Writer}.
 * <p>
 * This class constructs the required writer on its own, it's only needed to pass the {@link Writer} class it should
 * use. It also requires configuration object of type {@link RecordingConfigurations} and the name of the result file
 * (will also be created by the class itself)
 *
 * @author olha
 */
public class TakeScreenshotsThread extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TakeScreenshotsThread.class);

    private WebDriver driver;

    private String fileName;

    private String testName;

    private boolean run = true;

    private boolean testFailed = true;

    private RecordingConfigurations recordingConfigurations;

    private Writer writer;

    public TakeScreenshotsThread(WebDriver driver, Class<? extends Writer> writerClass, RecordingConfigurations recordingConfigurations,
        String testName)
        throws IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException
    {
        this.recordingConfigurations = recordingConfigurations;
        this.testName = testName;
        final File directory = new File(recordingConfigurations.tempFolderToStoreRecording());
        if (!directory.exists())
        {
            directory.mkdirs();
        }
        fileName = recordingConfigurations.tempFolderToStoreRecording()
                   + testName.replaceAll("\\s", "-").replaceAll(":", "-").replaceAll("/", "_") + "." + recordingConfigurations.format();
        this.writer = Writer.instantiate(writerClass, recordingConfigurations, fileName);
        this.driver = driver;
    }

    /**
     * Runs screenshot loop in background and writes the screenshots into files
     */
    @Override
    public synchronized void run()
    {
        // if writer construction was successful we can start the screenshot loop
        // in case there was an error while writer creation, the background thread should die as there is nothing to do
        // in the run method
        if (writer != null)
        {
            File lastFrameTempFile = null; // store the last good frame
            try
            {
                // Create a temp file to hold the last frame
                lastFrameTempFile = File.createTempFile("last_frame_", ".png");
            }
            catch (IOException e)
            {
                LOGGER.error("Could not create temp file for last screenshot, alert handling for test recording will be disabled.", e);
            }

            try
            {
                // Create a temp file to hold the last frame
                lastFrameTempFile = File.createTempFile("last_frame_", ".png");
            }
            catch (IOException e)
            {
                LOGGER.error("Could not create temp file for last screenshot, alert handling for test recording will be disabled.", e);
            }

            try
            {
                // try to start writer
                // in case writer start fails, the run method will exit
                writer.start();

                long turns = 0;
                long millis = 0;
                long duration = 0;

                // start screenshot loop
                while (run)
                {
                    long start = System.currentTimeMillis();

                    try
                    {
                        final WebDriver activeDriver = getActiveDriver();
                        if (activeDriver == null)
                        {
                            try
                            {
                                Thread.sleep(Math.max(recordingConfigurations.oneImagePerMilliseconds(), 100));
                            }
                            catch (final InterruptedException ignored)
                            {
                            }
                            continue;
                        }

                        long delay = Math.max(recordingConfigurations.oneImagePerMilliseconds(), duration);

                        // taking a screenshot while an alert is open will throw an exception and closes the alert, so
                        // it is checked here
                        if (alertIsPresent().apply(activeDriver) != null)
                        {
                            // write the last successful frame again, if it exists
                            if (lastFrameTempFile != null && lastFrameTempFile.exists() && lastFrameTempFile.length() > 0)
                            {
                                writer.write(lastFrameTempFile, delay);
                            }
                        }
                        else
                        {
                            File file = ((TakesScreenshot) activeDriver).getScreenshotAs(OutputType.FILE);
                            int cropWidth = ((Long) ((JavascriptExecutor) activeDriver).executeScript("return window.innerWidth;")).intValue();
                            int cropHeight = ((Long) ((JavascriptExecutor) activeDriver).executeScript("return window.innerHeight;")).intValue();
                            BufferedImage fullImage = ImageIO.read(file);
                            if (cropWidth > 0 && cropHeight > 0)
                            {
                                BufferedImage croppedImage = fullImage.getSubimage(0, 0, cropWidth, cropHeight);
                                ImageIO.write(croppedImage, "png", file);
                            }
                            writer.compressImageIfNeeded(file, recordingConfigurations.imageScaleFactor(), recordingConfigurations.imageQuality());
                            writer.write(file, delay);

                            // move this new frame to our temp file for the next iteration
                            if (lastFrameTempFile != null)
                            {
                                Files.move(file.toPath(), lastFrameTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                    catch (Throwable e)
                    {
                        if (e instanceof org.openqa.selenium.NoSuchSessionException || 
                           (e.getMessage() != null && (e.getMessage().contains("invalid session id") || e.getMessage().contains("Session not found") || e.getMessage().contains("unreachable")))) 
                        {
                            LOGGER.debug("Target browser closed manually or session expired. Screenshot loop gracefully stopping.");
                            run = false;
                            break;
                        }

                        // catching the exception prevents the video from failing
                        LOGGER.error("Screenshot could not be taken", e);
                    }

                    duration = System.currentTimeMillis() - start;
                    millis += duration;
                    turns++;

                    try
                    {
                        Thread.sleep(Math.max(recordingConfigurations.oneImagePerMilliseconds() - duration, 0));
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.error("thread didn't want to sleep", e);
                    }

                }

                boolean isGif = recordingConfigurations.format().equals("gif");
                if (recordingConfigurations.logInformationAboutRecording())
                {
                    AllureAddons.addToReport("average " + (isGif ? "gif" : "video") + " sequence recording creation duration = " + millis + " / " + turns + "="
                                             + millis / turns, "");
                }
                try
                {
                    writer.stop();
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Exception stopping video writer: {}", e.getMessage());
                }
                try
                {
                    final File tempRecording = new File(fileName);
                    File finalRecording = tempRecording;
                    if (tempRecording.exists())
                    {
                        final String runFolder = InteractiveConsoleEngine.getRunFolder();
                        final File runFolderDir = new File(recordingConfigurations.tempFolderToStoreRecording(), runFolder);
                        if (!runFolderDir.exists())
                        {
                            runFolderDir.mkdirs();
                        }
                        final File movedFile = new File(runFolderDir, tempRecording.getName());
                        try
                        {
                            Files.move(tempRecording.toPath(), movedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            finalRecording = movedFile;
                        }
                        catch (final IOException e)
                        {
                            LOGGER.warn("Could not move recording file to run folder: {}", e.getMessage());
                        }
                    }

                    try
                    {
                        InteractiveConsoleEngine.attachVideoToLatestExecutionLog(finalRecording, testName);
                    }
                    catch (final Exception ignored)
                    {
                    }

                    if (recordingConfigurations.appendAllRecordingsToAllureReport() || testFailed)
                    {
                        final String type = isGif ? "image/gif" : "video/mp4";
                        if (finalRecording.exists())
                        {
                            Allure.addAttachment(finalRecording.getName(), type, new FileInputStream(finalRecording), recordingConfigurations.format());
                        }

                        if (recordingConfigurations.deleteRecordingsAfterAddingToAllureReport() && finalRecording.exists())
                        {
                            finalRecording.delete();
                        }
                    }

                    // delete the file when configured and only if it wasn't deleted already
                    if (recordingConfigurations.deleteTempRecordings() && finalRecording.exists())
                    {
                        finalRecording.delete();
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            catch (IOException e1)
            {
                throw new RuntimeException(e1);
            }
            finally
            {
                // Clean up the temp file
                if (lastFrameTempFile != null)
                {
                    lastFrameTempFile.delete();
                }
            }
        }
    }

    private WebDriver getActiveDriver()
    {
        if (Neodymium.hasDriver())
        {
            return Neodymium.getDriver();
        }
        if (WebDriverRunner.hasWebDriverStarted())
        {
            return WebDriverRunner.getWebDriver();
        }
        return this.driver;
    }

    /**
     * Stops screenshot loop. Please, don't forget to call {@link Thread#join()} method after this to kill the thread
     *
     * @param testFailed
     *            {@link Boolean} if the filmed test failed (needed to decide whether the record should be attached to
     *            the allure report)
     */
    public void stopRun(boolean testFailed)
    {
        this.testFailed = testFailed;
        run = false;
    }
}
