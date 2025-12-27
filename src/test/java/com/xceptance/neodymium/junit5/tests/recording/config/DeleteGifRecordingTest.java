package com.xceptance.neodymium.junit5.tests.recording.config;

import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.recording.AbstractRecordingDeletionTest;
import org.junit.jupiter.api.BeforeAll;

public class DeleteGifRecordingTest extends AbstractRecordingDeletionTest
{
    public DeleteGifRecordingTest()
    {
        super(true);
    }

    @BeforeAll
    public static void form()
    {
        beforeClass("gif", true);
        configurationsClass = GifRecordingConfigurations.class;
    }

    @Override
    @NeodymiumTest
    public void test()
    {
        super.test();
    }
}
