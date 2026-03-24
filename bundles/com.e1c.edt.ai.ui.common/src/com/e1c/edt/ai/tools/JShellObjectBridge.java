/**
 *
 */
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JShellObjectBridge
{
    private static final Map<Integer, Object> objectStore = new HashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final Map<Integer, Set<Integer>> sessionObjectIds = new ConcurrentHashMap<>();

    public static int store(int sessionId, Object obj)
    {
        int id = counter.incrementAndGet();
        objectStore.put(id, obj);
        sessionObjectIds.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(id);
        return id;
    }

    @SuppressWarnings("unchecked")
    public static <T> T retrieve(int sessionId, int id)
    {
        Set<Integer> sessionIds = sessionObjectIds.get(sessionId);
        if (sessionIds != null && sessionIds.contains(id))
        {
            return (T)objectStore.get(id);
        }
        return null;
    }

    public static void releaseSession(int sessionId)
    {
        Set<Integer> ids = sessionObjectIds.remove(sessionId);
        if (ids != null)
        {
            ids.forEach(objectStore::remove);
        }
    }
}
