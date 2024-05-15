/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

public interface IJson
{
    String serialize(Object src);

    <T> Optional<T> deserialize(String json, Class<T> classOfT);
}
