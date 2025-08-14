/**
 *
 */
package com.e1c.edt.ai;

import java.io.IOException;

import com.e1c.edt.ai.assistent.model.TokenHealing;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class CustomTypeAdapterFactory
    implements TypeAdapterFactory
{
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
    {
        if (type.getRawType() != TokenHealing.class)
        {
            return null;
        }

        var defaultAdapter = (TypeAdapter<TokenHealing>)gson.getDelegateAdapter(this, type);
        return (TypeAdapter<T>)new TokenHealingAdapter(defaultAdapter);

    }

    private static class TokenHealingAdapter
        extends TypeAdapter<TokenHealing>
    {
        protected TypeAdapter<TokenHealing> defaultAdapter;

        public TokenHealingAdapter(TypeAdapter<TokenHealing> defaultAdapter)
        {
            this.defaultAdapter = defaultAdapter;
        }

        @Override
        public TokenHealing read(JsonReader reader) throws IOException
        {
            return defaultAdapter.read(reader);
        }

        @Override
        public void write(JsonWriter writer, TokenHealing value) throws IOException
        {
            if (value == null || value == TokenHealing.NONE)
            {
                synchronized (writer)
                {
                    var serializeNulls = writer.getSerializeNulls();
                    writer.setSerializeNulls(true);
                    try
                    {
                        writer.nullValue();
                    }
                    finally
                    {
                        writer.setSerializeNulls(serializeNulls);
                    }
                }
            }
            else
            {
                writer.value(value.toString().toLowerCase());
            }
        }
    }
}
