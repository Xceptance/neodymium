package com.xceptance.neodymium.recording;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.recording.writers.Writer;
import com.xceptance.neodymium.util.AllureAddons;

import io.qameta.allure.Allure;

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
    private WebDriver driver;

    private String fileName;

    private boolean run = true;

    private boolean testFailed = true;

    private RecordingConfigurations recordingConfigurations;

    private Writer writer;

    public TakeScreenshotsThread(WebDriver driver, Class<? extends Writer> writerClass, RecordingConfigurations recordingConfigurations,
        String testName) throws IOException
    {
        this.recordingConfigurations = recordingConfigurations;
        fileName = recordingConfigurations.tempFolderToStoreRecoring()
                   + testName.replaceAll("\\s", "-").replaceAll(":", "-").replaceAll("/", "_") + "." + recordingConfigurations.format();
        this.writer = Writer.instantiate(writerClass, recordingConfigurations, fileName);
        File directory = new File(recordingConfigurations.tempFolderToStoreRecoring());
        if (!directory.exists())
        {
            directory.mkdir();
        }
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
            try
            {
                // try to start writer
                // in case writer start fails, the run method will exit
                writer.start();

                long turns = 0;
                long millis = 0;
                // start screenshot loop
                while (run)
                {
                    long start = new Date().getTime();

                    File file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    writer.compressImageIfNeeded(file, recordingConfigurations.imageScaleFactor(), recordingConfigurations.imageQuality());
                    writer.write(file);

                    long duration = new Date().getTime() - start;
                    millis += duration;
                    turns++;
                    long sleep = recordingConfigurations.oneImagePerMilliseconds() - duration;
                    try
                    {
                        Thread.sleep(sleep > 0 ? sleep : 0);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }

                }
                boolean isGif = recordingConfigurations.format().equals("gif");
                AllureAddons.addToReport("avarage " + (isGif ? "gif" : "video") + " sequence recording creation duration = " + millis + " / " + turns + "="
                                         + millis / turns, "");
                writer.stop();
                try
                {
                    if (recordingConfigurations.appendAllRecordingsToReport() || testFailed)
                    {

                        String type = isGif ? "image/gif" : "video/mp4";
                        Allure.addAttachment(fileName, type, new FileInputStream(fileName), recordingConfigurations.format());

                    }
                    if (recordingConfigurations.deleteRecordingsAfterAddingToAllureReport())
                    {
                        new File(fileName).delete();
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
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
