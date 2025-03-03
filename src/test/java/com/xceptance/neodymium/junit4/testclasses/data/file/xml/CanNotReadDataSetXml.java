package com.xceptance.neodymium.junit4.testclasses.data.file.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@DataFile("can/not/read/data/set/xml/DoesNotExist.xml")
public class CanNotReadDataSetXml
{
    @Test()
    public void test()
    {
        Neodymium.getData();
    }
}
