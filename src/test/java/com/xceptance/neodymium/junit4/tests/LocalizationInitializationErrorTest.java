package com.xceptance.neodymium.junit4.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class LocalizationInitializationErrorTest extends NeodymiumTest
{
    private static File tempConfigFile;

    @BeforeClass
    public static void createLocalizationFile() throws IOException
    {
        tempConfigFile = File.createTempFile("localization", ".yaml", new File("./config/"));
        tempFiles.add(tempConfigFile);

        // set system property to change default localization file to the new created
        System.setProperty("neodymium.localization.file", tempConfigFile.getPath());

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempConfigFile)));
        bw.write("default:");
        bw.newLine();
        bw.write(" key1: default");
        bw.newLine();
        bw.write("fr_FR:");
        bw.newLine();
        bw.write(" key1: fr_FR");
        bw.newLine();
        bw.write(" Yes: ja");
        bw.newLine();
        bw.close();
    }

    @Test
    public void testAssertionErrorWhenKeyIsUnknown()
    {
        Assert.assertThrows(RuntimeException.class, () -> {
            Neodymium.localizedText("key1");
        });
    }
}
