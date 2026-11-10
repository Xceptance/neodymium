package com.xceptance.neodymium.junit5.tests.recording.automatic;

import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit5.tests.recording.AbstractRecordingTest;
import org.junit.jupiter.api.BeforeAll;

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
        configurationsClass.put(Thread.currentThread(), GifRecordingConfigurations.class);
    }
}
