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
package com.xceptance.neodymium.junit5.tests.utils;

import java.util.Optional;
import java.util.Set;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

public class MockedTestDescriptor implements TestDescriptor
{
    private String uniqueId;

    private String displayName;

    public MockedTestDescriptor(String uniqueId, String displayName)
    {
        this.uniqueId = uniqueId;
        this.displayName = displayName;
    }

    @Override
    public UniqueId getUniqueId()
    {
        return UniqueId.parse(uniqueId);
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public Set<TestTag> getTags()
    {
        // TODO Auto-generated method stub
        return Set.of();
    }

    @Override
    public Optional<TestSource> getSource()
    {
        // TODO Auto-generated method stub
        return Optional.ofNullable(null);
    }

    @Override
    public Optional<TestDescriptor> getParent()
    {
        // TODO Auto-generated method stub
        return Optional.ofNullable(null);
    }

    @Override
    public void setParent(TestDescriptor parent)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<? extends TestDescriptor> getChildren()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addChild(TestDescriptor descriptor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeChild(TestDescriptor descriptor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFromHierarchy()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Type getType()
    {
        // TODO Auto-generated method stub
        return Type.TEST;
    }

    @Override
    public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
