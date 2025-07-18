/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Collections
{
    public static <T> List<Collection<T>> split(Collection<T> collection, int partitionSize)
    {
        var result = new ArrayList<Collection<T>>();
        var size = collection.size();
        if (size == 0)
        {
            return result;
        }

        if (partitionSize <= 0 || partitionSize >= size)
        {
            result.add(collection);
            return result;
        }

        var partition = new ArrayList<T>(partitionSize);
        var indexInPartition = 0;
        var remaining = size;
        for (T val : new ArrayList<>(collection))
        {
            if (indexInPartition == partitionSize)
            {
                result.add(partition);
                partition = new ArrayList<>(remaining > partitionSize ? partitionSize : remaining);
                indexInPartition = 0;
            }

            partition.add(val);
            indexInPartition++;
            remaining--;
        }

        if (!partition.isEmpty())
        {
            result.add(partition);
        }

        return result;
    }
}
