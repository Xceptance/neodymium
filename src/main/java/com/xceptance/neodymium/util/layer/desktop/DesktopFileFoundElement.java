/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.util.layer.desktop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.xceptance.neodymium.util.layer.ElementCondition;

/**
 * Represents a desktop resource defined as a file path on disk.
 * Allows AssertAction to verify file existence and file contents
 * during desktop automation playbooks.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class DesktopFileFoundElement extends DesktopFoundElement
{
    private final String filePath;

    /**
     * Constructs a new file-based desktop resource representation.
     *
     * @param filePath the absolute path of the target file on disk
     */
    public DesktopFileFoundElement(final String filePath)
    {
        super(0, 0);
        this.filePath = filePath;
    }

    @Override
    public boolean exists()
    {
        return new File(filePath).exists();
    }

    @Override
    public boolean isVisible()
    {
        return exists();
    }

    @Override
    public boolean isDisplayed()
    {
        return exists();
    }

    @Override
    public String getText()
    {
        try
        {
            return Files.readString(Path.of(filePath));
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to read file contents for assertion: " + filePath, e);
        }
    }

    @Override
    public String getTextContent()
    {
        return getText();
    }

    @Override
    public boolean matchesCondition(final ElementCondition condition)
    {
        if (condition.getType() == ElementCondition.Type.EXIST)
        {
            return exists();
        }
        if (condition.getType() == ElementCondition.Type.TEXT_CONTAINS)
        {
            return getText().contains(condition.getParam1());
        }
        return false;
    }

    @Override
    public void assertCondition(final ElementCondition condition)
    {
        if (condition.getType() == ElementCondition.Type.EXIST)
        {
            if (!exists())
            {
                throw new AssertionError("Assertion failed: expected file to exist, but it was not found: " + filePath);
            }
        }
        else if (condition.getType() == ElementCondition.Type.TEXT_CONTAINS)
        {
            final String content = getText();
            if (!content.contains(condition.getParam1()))
            {
                throw new AssertionError(String.format("Assertion failed: expected file contents to contain '%s', but got: '%s'", condition.getParam1(), content));
            }
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported condition for file assertions: " + condition.getType());
        }
    }
}
