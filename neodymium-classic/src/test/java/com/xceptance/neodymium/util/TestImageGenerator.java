/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Assert;

/**
 * This class generates random images which can be uploaded.
 * 
 * @author Xceptance Software Technologies
 *
 */
public class TestImageGenerator
{
	private static void testImage(BufferedImage image)
	{
	    int red = new Color(255, 0, 0).getRGB();
	    int green = new Color(0, 255, 0).getRGB();
	    int blue = new Color(0, 0, 255).getRGB();
	    int[] colors = {red, green, blue};
	   
	    int width = image.getWidth();
	    int height= image.getHeight();
	    
	    for(int i = 0; i < width; i++)
	    {
	        for(int j = 0; j < height; j++)
	        {
	            int color = colors[(int) Math.floor(Math.random()*colors.length)];
	            image.setRGB(i, j, color);
	        }
	    }
	}
	
    public static File generateImage()
    {
    	int width = 1920;
    	int height = 1080;
    	
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);	
    	testImage(image);    	
		File newImageFile = null;
		
		try
    	{
			long timestamp = System.currentTimeMillis();
			newImageFile = File.createTempFile("Neodymium-Writer-Test-Image-" + Long.toString(timestamp), ".jpg");
			newImageFile.createNewFile();
			newImageFile.deleteOnExit();
			
			ImageIO.write(image, "jpg", newImageFile);
    	}
		catch(Exception e)
		{
			Assert.fail("Generating a new image failed");
		}
		
		return newImageFile;
    }
}
