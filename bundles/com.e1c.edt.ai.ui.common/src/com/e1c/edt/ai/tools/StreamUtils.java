/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utility class for working with {@link Stream}.
 */
public final class StreamUtils
{

    private StreamUtils()
    {
        // Utility class - prevent instantiation
    }

    /**
     * Returns a stream consisting of distinct elements from this stream,
     * comparing elements by a key extracted by the provided function.
     *
     * @param source stream to filter
     * @param keyExtractor function to extract key from element
     * @param <T> type of stream elements
     * @return new stream with distinct elements by key
     */
    public static <T> Stream<T> distinctBy(Stream<T> source, Function<? super T, ?> keyExtractor)
    {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return source.filter(t -> seen.add(keyExtractor.apply(t)));
    }
}
