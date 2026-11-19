package com.xceptance.neodymium.common.recording;

import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.common.recording.writers.GifSequenceWriter;
import com.xceptance.neodymium.common.recording.writers.VideoWriter;
import com.xceptance.neodymium.util.Neodymium;
import org.aeonbits.owner.ConfigFactory;
import org.openqa.selenium.WebDriver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;

/**
 * Wrapper class to ease work with {@link TakeScreenshotsThread}. This class also ensures the thread safety of {@link GifRecordingConfigurations} and
 * {@link VideoRecordingConfigurations}
 *
 * @author Xceptance Software Technologies
 */
public class FilmTestExecution
{
    private static final Map<Thread, VideoRecordingConfigurations> CONTEXTS_VIDEO = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<Thread, GifRecordingConfigurations> CONTEXTS_GIF = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<String, TakeScreenshotsThread> GIF_THREADS = new ConcurrentHashMap<>();

    private static final Map<String, TakeScreenshotsThread> VIDEO_THREADS = new ConcurrentHashMap<>();

    public final static String TEMPORARY_CONFIG_FILE_PROPERTY_NAME = "recording.temporaryConfigFile";

    /**
     * Gets {@link RecordingConfigurations} for current thread
     *
     * @param configurationClass
     *     class of desired configurations ({@link VideoRecordingConfigurations} or {@link GifRecordingConfigurations})
     * @return gif or video configurations in form of {@link RecordingConfigurations} object
     */
    public static RecordingConfigurations getContext(Class<? extends RecordingConfigurations> configurationClass)
    {
        // the property needs to be a valid URI in order to satisfy the Owner framework
        if (null == ConfigFactory.getProperty(TEMPORARY_CONFIG_FILE_PROPERTY_NAME))
        {
            ConfigFactory.setProperty(TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:this/path/should/never/exist/noOneShouldCreateMe.properties");
        }
        if (configurationClass.equals(VideoRecordingConfigurations.class))
        {
            return CONTEXTS_VIDEO.computeIfAbsent(Thread.currentThread(), key -> {
                return ConfigFactory.create(VideoRecordingConfigurations.class);
            });
        }
        return CONTEXTS_GIF.computeIfAbsent(Thread.currentThread(), key -> {
            return ConfigFactory.create(GifRecordingConfigurations.class);
        });
    }

    /**
     * Removes configuration from current thread to be able to compute a new one, because computing a new one is only possible if the there is no configuration
     * already.
     */
    public static void clearThreadContexts()
    {
        CONTEXTS_GIF.remove(Thread.currentThread());
        CONTEXTS_VIDEO.remove(Thread.currentThread());
    }

    /**
     * Gets {@link GifRecordingConfigurations} for current thread
     *
     * @return gif configurations in form of {@link GifRecordingConfigurations} object
     */
    public static GifRecordingConfigurations getContextGif()
    {
        return (GifRecordingConfigurations) getContext(GifRecordingConfigurations.class);
    }

    /**
     * Gets {@link VideoRecordingConfigurations} for current thread
     *
     * @return gif configurations in form of {@link VideoRecordingConfigurations} object
     */
    public static VideoRecordingConfigurations getContextVideo()
    {
        return (VideoRecordingConfigurations) getContext(VideoRecordingConfigurations.class);
    }

    /**
     * Creates {@link TakeScreenshotsThread} for the specific test
     *
     * @param testName
     *     name of the test to create the {@link TakeScreenshotsThread} for
     * @param isGif
     *     should the desired {@link TakeScreenshotsThread} create a gif
     * @return constructed {@link TakeScreenshotsThread}
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    private static TakeScreenshotsThread createTakeScreenshotsThread(String testName, boolean isGif)
    {
        RecordingConfigurations config = getContext((isGif ? GifRecordingConfigurations.class
            : VideoRecordingConfigurations.class));

        if (!config.enableFilming() || !Neodymium.hasDriver())
        {
            return null;
        }

        try
        {
            if (isGif)
            {
                assertTrue("recording property gif.oneImagePerMilliseconds value is out of bounds", config.oneImagePerMilliseconds() >= 0);
                assertTrue("recording property gif.imageQuality value is out of bounds", (0 <= config.imageQuality()) && (config.imageQuality() <= 1.0));
                assertTrue("recording property gif.imageScaleFactor value is out of bounds",
                           (0 < config.imageScaleFactor()) && (config.imageScaleFactor() <= 1.0));
            }
            else
            {
                assertTrue("recording property video.oneImagePerMilliseconds value is out of bounds", config.oneImagePerMilliseconds() >= 0);
                assertTrue("recording property video.imageQuality value is out of bounds", (0 <= config.imageQuality()) && (config.imageQuality() <= 1.0));
                assertTrue("recording property video.imageScaleFactor value is out of bounds",
                           (0 < config.imageScaleFactor()) && (config.imageScaleFactor() <= 1.0));
            }

            return new TakeScreenshotsThread(Neodymium.getDriver(), isGif ? GifSequenceWriter.class
                : VideoWriter.class, getContext((isGif ? GifRecordingConfigurations.class
                : VideoRecordingConfigurations.class)), testName);
        }
        catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
            | SecurityException e)
        {
            if (e instanceof FileNotFoundException)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            throw new RuntimeException("thread couldn't be created", e);
        }
    }

    /**
     * Creates {@link TakeScreenshotsThread} object and puts it in the thread safe map
     *
     * @param testName
     *     name of the test to create the {@link TakeScreenshotsThread} for
     * @param isGif
     *     should the desired {@link TakeScreenshotsThread} create a gif
     * @return constructed {@link TakeScreenshotsThread}
     */
    private static TakeScreenshotsThread createThread(final String testName, final boolean isGif)
    {
        if (testName == null)
        {
            return null;
        }
        final Map<String, TakeScreenshotsThread> threads = isGif ? GIF_THREADS : VIDEO_THREADS;
        return threads.computeIfAbsent(testName, key -> {
            return createTakeScreenshotsThread(testName, isGif);
        });
    }

    /**
     * Gets the {@link TakeScreenshotsThread} for the specific test and removes it
     *
     * @param isGif
     *     should the desired {@link TakeScreenshotsThread} create a gif
     * @return {@link TakeScreenshotsThread} for the current thread
     */
    private static TakeScreenshotsThread popThread(final String testName, final boolean isGif)
    {
        if (testName == null)
        {
            return null;
        }
        final Map<String, TakeScreenshotsThread> threads = isGif ? GIF_THREADS : VIDEO_THREADS;
        final TakeScreenshotsThread thread = threads.get(testName);
        threads.remove(testName);
        return thread;
    }

    /**
     * Starts a gif recording for the current test
     *
     * @param testName
     *     name of the test
     */
    public static void startGifRecording(String testName)
    {
        TakeScreenshotsThread thread = createThread(testName, true);
        if (thread != null)
        {
            thread.start();
        }
    }

    /**
     * Gets names of all currently running in background {@link TakeScreenshotsThread} for gifs
     *
     * @return {@link List} of the names
     */
    public static List<String> getNamesOfAllCurrentGifRecordings()
    {
        ArrayList<String> recordingNames = new ArrayList<>();
        recordingNames.addAll(GIF_THREADS.keySet());
        return recordingNames;
    }

    /**
     * Gets names of all currently running in background {@link TakeScreenshotsThread} for videos
     *
     * @return {@link List} of the names
     */
    public static List<String> getNamesOfAllCurrentVideoRecordings()
    {
        ArrayList<String> recordingNames = new ArrayList<>();
        recordingNames.addAll(VIDEO_THREADS.keySet());
        return recordingNames;
    }

    /**
     * Checks whether a video or gif recording is currently running.
     *
     * @return true if an active recording thread is running, false otherwise
     */
    public static boolean isRecording()
    {
        return isVideoRecording() || isGifRecording();
    }

    /**
     * Checks whether an active recording is running for the specified {@link WebDriver} instance.
     *
     * @param driver
     *            the WebDriver instance to check
     * @return true if a recording thread is actively filming this driver, false otherwise
     */
    public static boolean isRecording(final WebDriver driver)
    {
        if (driver != null)
        {
            return isRecordingForDriver(VIDEO_THREADS, driver) || isRecordingForDriver(GIF_THREADS, driver);
        }
        return isRecording();
    }

    /**
     * Checks whether a video recording is currently running.
     *
     * @return true if an active video recording thread is running, false otherwise
     */
    public static boolean isVideoRecording()
    {
        return hasActiveThread(VIDEO_THREADS);
    }

    /**
     * Checks whether a gif recording is currently running.
     *
     * @return true if an active gif recording thread is running, false otherwise
     */
    public static boolean isGifRecording()
    {
        return hasActiveThread(GIF_THREADS);
    }

    private static boolean hasActiveThread(final Map<String, TakeScreenshotsThread> threads)
    {
        for (final TakeScreenshotsThread thread : threads.values())
        {
            if (thread != null && thread.isAlive())
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isRecordingForDriver(final Map<String, TakeScreenshotsThread> threads, final WebDriver driver)
    {
        final WebDriverStateContainer container = Neodymium.getWebDriverStateContainer();
        for (final TakeScreenshotsThread thread : threads.values())
        {
            if (thread != null && thread.isAlive())
            {
                final WebDriver threadDriver = thread.getDriver();
                if (threadDriver != null)
                {
                    if (threadDriver == driver)
                    {
                        return true;
                    }
                    if (container != null && container.matches(threadDriver) && container.matches(driver))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Starts a video recording for the current test
     *
     * @param testName
     *     name of the test
     */
    public static void startVideoRecording(String testName)
    {
        TakeScreenshotsThread thread = createThread(testName, false);
        if (thread != null)
        {
            thread.start();
        }
    }

    /**
     * Finishes gif filming for the current test
     *
     * @param testName
     *     name of the test to finish filming for
     * @param testFailed
     *     {@link Boolean} value of test test success (needed to decide whether the gif should be attached to allure report)
     */
    public static void finishGifFilming(String testName, boolean testFailed)
    {
        TakeScreenshotsThread thread = popThread(testName, true);
        if (thread != null)
        {
            thread.stopRun(testFailed);
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Finishes video filming for the current test
     *
     * @param testName
     *     name of the test to finish filming for
     * @param testFailed
     *     {@link Boolean} value of test test success (needed to decide whether the video should be attached to allure report)
     */
    public static void finishVideoFilming(String testName, boolean testFailed)
    {
        TakeScreenshotsThread thread = popThread(testName, false);
        if (thread != null)
        {
            thread.stopRun(testFailed);
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
