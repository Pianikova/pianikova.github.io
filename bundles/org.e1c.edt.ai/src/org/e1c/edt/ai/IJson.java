/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IJson
{
    String serialize(Object src);

    <T> T deserialize(String json, Class<T> classOfT);
}
