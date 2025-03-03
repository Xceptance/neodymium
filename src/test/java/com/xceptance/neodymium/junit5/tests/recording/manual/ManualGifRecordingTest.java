package com.xceptance.neodymium.junit5.tests.recording.manual;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class ManualGifRecordingTest extends AbstractRecordingTest
{
    public ManualGifRecordingTest()
    {
        super(true);
    }

    @BeforeAll
    public static void form()
    {
        beforeClass("gif", false);
        configurationsClass = GifRecordingConfigurations.class;
    }

    @BeforeEach
    public void startFilming()
    {
        FilmTestExecution.startGifRecording(UUID.randomUUID().toString());
    }

    @AfterEach
    public void finishFilming()
    {
        FilmTestExecution.finishGifFilming(uuid, false);
    }
}
