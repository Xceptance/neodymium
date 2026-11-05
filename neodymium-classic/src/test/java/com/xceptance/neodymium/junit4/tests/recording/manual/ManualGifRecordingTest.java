package com.xceptance.neodymium.junit4.tests.recording.manual;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class ManualGifRecordingTest extends AbstractRecordingTest
{
    public ManualGifRecordingTest()
    {
        super(true);
    }

    @BeforeClass
    public static void form()
    {
        beforeClass("gif", false);
        configurationsClass.put(Thread.currentThread(), GifRecordingConfigurations.class);
    }

    @Before
    public void startFilming()
    {
        FilmTestExecution.startGifRecording(UUID.randomUUID().toString());
    }

    @After
    public void finishFilming()
    {
        FilmTestExecution.finishGifFilming(uuid.get(Thread.currentThread()), false);
    }
}
