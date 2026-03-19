/**
 *
 */
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JShellObjectBridge
{
    private static final Map<Integer, Object> objectStore = new HashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(0);

    public static int store(Object obj)
    {
        int id = counter.incrementAndGet();
        objectStore.put(id, obj);
        return id;
    }

    @SuppressWarnings("unchecked")
    public static <T> T retrieve(int id)
    {
        return (T)objectStore.get(id);
    }

    public static void remove(int id)
    {
        objectStore.remove(id);
    }
}
