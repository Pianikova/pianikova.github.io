/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Stream;

public final class StreamUtils
{
    public static <T> Stream<T> distinctBy(Stream<T> source, Function<? super T, ?> keyExtractor)
    {
        var seen = new HashSet<Object>();
        return source.filter(t -> seen.add(keyExtractor.apply(t)));
    }
}
