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
