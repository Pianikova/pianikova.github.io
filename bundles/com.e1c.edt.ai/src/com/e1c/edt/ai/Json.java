/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import com.e1c.edt.ai.assistent.model.TokenHealing;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class Json
    implements IJson
{
    private final Gson gson;
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    @SuppressWarnings({ "unchecked", "unused", "rawtypes" })
    public Json()
    {
        //  @formatter:off
        gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapterFactory(new <Integer> OptionalTypeAdapterFactory(
                new TypeToken<Optional<Integer>>() { /**/ },
                new TypeToken<Integer>() { /**/ }))
            .registerTypeAdapterFactory(new <Boolean> OptionalTypeAdapterFactory(
                new TypeToken<Optional<Boolean>>() { /**/ },
                new TypeToken<Boolean>() { /**/ }))
            .registerTypeAdapterFactory(new <Double> OptionalTypeAdapterFactory(
                new TypeToken<Optional<Double>>() { /**/ },
                new TypeToken<Double>() { /**/ }))
            .registerTypeAdapterFactory(new <String> OptionalTypeAdapterFactory(
                new TypeToken<Optional<String>>() { /**/ },
                new TypeToken<String>() { /**/ }))
            .registerTypeAdapterFactory(new <URL> OptionalTypeAdapterFactory(
                new TypeToken<Optional<URL>>() { /**/ },
                new TypeToken<URL>() { /**/ }))
            .registerTypeAdapterFactory(new <TokenHealing> OptionalTypeAdapterFactory(
                new TypeToken<Optional<TokenHealing>>() { /**/ },
                new TypeToken<TokenHealing>() { /**/ }))
            .registerTypeAdapter(new TypeToken<List<String>>() { /**/ }.getType(), new StringOrStringListDeserializer())
            .create();
     // @formatter:on
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
        try
        {
            return Optional.ofNullable(gson.fromJson(json, classOfT));
        }
        catch (Exception error)
        {
            return Optional.empty();
        }
    }

    @Override
    public String formatJson(String json)
    {
        try
        {
            return prettyGson.toJson(JsonParser.parseString(json));
        }
        catch (Exception error)
        {
            return json;
        }
    }
}
