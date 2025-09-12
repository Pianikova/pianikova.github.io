/**
 *
 */
package com.e1c.edt.ai;

import java.io.IOException;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class OptionalTypeAdapterFactory<TValue>
    implements TypeAdapterFactory
{
    private final TypeToken<Optional<TValue>> typeToken;
    private final TypeToken<TValue> baseTypeToken;

    public OptionalTypeAdapterFactory(TypeToken<Optional<TValue>> typeToken, TypeToken<TValue> baseTypeToken)
    {
        Preconditions.checkNotNull(typeToken);
        Preconditions.checkNotNull(baseTypeToken);
        this.typeToken = typeToken;
        this.baseTypeToken = baseTypeToken;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
    {
        if (!type.equals(typeToken))
        {
            return null;
        }

        var defaultAdapter = gson.getDelegateAdapter(this, baseTypeToken);
        return new OptionalAdapter(defaultAdapter);
    }

    private static class OptionalAdapter<TValue>
        extends TypeAdapter<Optional<TValue>>
    {
        protected TypeAdapter<TValue> defaultAdapter;

        public OptionalAdapter(TypeAdapter<TValue> defaultAdapter)
        {
            this.defaultAdapter = defaultAdapter;
        }

        @Override
        public Optional<TValue> read(JsonReader reader) throws IOException
        {
            return Optional.ofNullable(defaultAdapter.read(reader));
        }

        @Override
        public void write(JsonWriter writer, Optional<TValue> value) throws IOException
        {
            if (value == null)
            {
                writer.nullValue();
                return;
            }

            if (value.isPresent())
            {
                defaultAdapter.write(writer, value.get());
                return;
            }

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
}
