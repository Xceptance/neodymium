/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
