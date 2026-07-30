/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public interface IJson
{
    String serialize(Object src);

    <T> Optional<T> deserialize(String json, Class<T> classOfT);

    String formatJson(String json);

    /**
     * Removes insignificant whitespace from valid JSON.
     *
     * @param json source text
     * @return compact JSON, or the source text when it is not valid JSON
     */
    String compactJson(String json);
}
