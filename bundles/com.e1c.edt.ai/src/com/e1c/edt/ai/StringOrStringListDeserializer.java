/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Custom deserializer for handling both String and List<String> types.
 * Converts a single string value to a list with one element.
 */
public class StringOrStringListDeserializer
    implements JsonDeserializer<List<String>>
{
    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException
    {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString())
        {
            // Handle single string value - convert to list with one element
            List<String> result = new ArrayList<>();
            result.add(json.getAsString());
            return result;
        }
        else if (json.isJsonArray())
        {
            // Handle array of strings
            return context.deserialize(json, List.class);
        }
        else if (json.isJsonNull())
        {
            // Handle null value
            return null;
        }
        throw new JsonParseException("Expected String or List<String>"); //$NON-NLS-1$
    }
}
