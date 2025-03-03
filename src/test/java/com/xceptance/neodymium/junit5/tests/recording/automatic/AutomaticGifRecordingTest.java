package com.xceptance.neodymium.junit5.tests.recording.automatic;

import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class AutomaticGifRecordingTest extends AbstractRecordingTest
{
    public AutomaticGifRecordingTest()
    {
        super(true);
    }

    @BeforeAll
    public static void form()
    {
        beforeClass("gif", true);
        configurationsClass = GifRecordingConfigurations.class;
    }
}
