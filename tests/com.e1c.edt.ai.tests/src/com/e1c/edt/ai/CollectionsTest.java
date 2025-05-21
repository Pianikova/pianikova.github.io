/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class CollectionsTest
{
    private static final ArrayList<Integer> collection = new ArrayList<>();

    static
    {
        collection.add(1);
        collection.add(2);
        collection.add(3);
        collection.add(4);
        collection.add(5);
    }

    @Test
    public void shouldSpilt()
    {
        // Given

        // When
        var actual = Collections.split(collection, 2);

        // Then
        Assert.assertEquals(actual.size(), 3);
        Assert.assertArrayEquals(new Integer[] { 1, 2 }, actual.get(0).toArray());
        Assert.assertArrayEquals(new Integer[] { 3, 4 }, actual.get(1).toArray());
        Assert.assertArrayEquals(new Integer[] { 5 }, actual.get(2).toArray());
    }

    @Test
    public void shouldSpiltWhenPartionSizeIsZero()
    {
        // Given

        // When
        var actual = Collections.split(collection, 0);

        // Then
        Assert.assertEquals(1, actual.size());
        Assert.assertArrayEquals(collection.toArray(), actual.get(0).toArray());
    }

    @Test
    public void shouldSpiltWhenCollectionIsEmpty()
    {
        // Given

        // When
        var actual = Collections.split(new ArrayList<>(), 2);

        // Then
        Assert.assertEquals(0, actual.size());
    }

    @Test
    public void shouldSpiltWhenPartionSizeIsTheSameAsCollectionSize()
    {
        // Given

        // When
        var actual = Collections.split(collection, 5);

        // Then
        Assert.assertEquals(1, actual.size());
        Assert.assertArrayEquals(collection.toArray(), actual.get(0).toArray());
    }

    @Test
    public void shouldSpiltWhenPartionSizeIsMoreTheCollectionSize()
    {
        // Given

        // When
        var actual = Collections.split(collection, 7);

        // Then
        Assert.assertEquals(1, actual.size());
        Assert.assertArrayEquals(collection.toArray(), actual.get(0).toArray());
    }
}
