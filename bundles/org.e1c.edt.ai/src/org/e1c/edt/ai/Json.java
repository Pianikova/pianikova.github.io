/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

public class Json implements IJson
{
    private final Gson gson;

    public Json()
    {
        gson = new Gson();
    }

    @Override
    public String serialize(Object src)
    {
        Preconditions.checkNotNull(src);
        return gson.toJson(src);
    }

    @Override
    public <T> Optional<T> deserialize(String json, Class<T> classOfT)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(classOfT);
        return Optional.ofNullable(gson.fromJson(json, classOfT));
    }
}
