package com.xceptance.neodymium.junit4.tests.recording.config;

import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingDeletionTest;
import org.junit.BeforeClass;

public class DeleteGifRecordingTest extends AbstractRecordingDeletionTest
{
    public DeleteGifRecordingTest()
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
