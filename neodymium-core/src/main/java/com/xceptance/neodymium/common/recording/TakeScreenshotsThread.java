package com.xceptance.neodymium.common.recording;

/**
 * @deprecated Use {@link org.neodymium.common.recording.TakeScreenshotsThread} instead.
 */
@Deprecated
public class TakeScreenshotsThread extends org.neodymium.common.recording.TakeScreenshotsThread
{
    public TakeScreenshotsThread(org.openqa.selenium.WebDriver driver, java.lang.Class<? extends org.neodymium.common.recording.writers.Writer> writerClass, org.neodymium.common.recording.config.RecordingConfigurations recordingConfigurations, java.lang.String testName) throws java.io.IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, java.lang.reflect.InvocationTargetException
    {
        super(driver, writerClass, recordingConfigurations, testName);
    }
}
