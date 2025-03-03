package com.xceptance.neodymium.junit4.tests.recording.automatic;

import org.junit.BeforeClass;

import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class AutomaticGifRecordingTest extends AbstractRecordingTest
{
    public AutomaticGifRecordingTest()
    {
        super(true);
    }

    @BeforeClass
    public static void form()
    {
        beforeClass("gif", true);
        configurationsClass = GifRecordingConfigurations.class;
    }
}
