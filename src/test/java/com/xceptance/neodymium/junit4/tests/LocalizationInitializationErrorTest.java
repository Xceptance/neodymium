package com.xceptance.neodymium.junit4.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.localization.LocalizationInitializationErrorTestClass;

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
        Result result = JUnitCore.runClasses(LocalizationInitializationErrorTestClass.class);
        checkFail(result, 1, 0, 1);
        Assert.assertEquals("Localization keys must be of type String. (e.g. use \"Yes\" instead of Yes as key. This is due to YAML auto conversion.)", result.getFailures().get(0).getException().getCause().getMessage());
    }
}
