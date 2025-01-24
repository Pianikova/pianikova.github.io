/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class HashTools
    implements IHashTools
{
    private final Provider<MessageDigest> messageDigestProvider;

    @Inject
    public HashTools(Provider<MessageDigest> messageDigestProvider)
    {
        Preconditions.checkNotNull(messageDigestProvider);
        this.messageDigestProvider = messageDigestProvider;
    }

    @Override
    public MessageDigest clone(MessageDigest hash)
    {
        try
        {
            return (MessageDigest)hash.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return hash;
        }
    }

    @Override
    public String format(MessageDigest hash, boolean includePrefix)
    {
        var bytes = clone(hash).digest();
        var text = new StringBuilder((includePrefix ? (hash.getAlgorithm().length() + 1) : 0) + bytes.length * 2);
        if (includePrefix)
        {
            text.append(hash.getAlgorithm());
            text.append(':');
        }

        for (var i = 0; i < bytes.length; i++)
        {
            var bt = bytes[i];
            if ((0xff & bt) < 0x10)
            {
                text.append('0');
            }

            text.append(Integer.toHexString(0xFF & bt));
        }

        return text.toString();
    }

    @Override
    public void scanTextStream(CharBuffer buffer, Consumer<byte[]> consumer, InputStream inputStream, Charset charset,
        Predicate<Character> filter)
        throws UnsupportedEncodingException, IOException
    {
        Preconditions.checkNotNull(buffer);
        Preconditions.checkNotNull(consumer);
        Preconditions.checkNotNull(inputStream);
        Preconditions.checkNotNull(charset);
        if (filter == null)
        {
            filter = (ch) -> !Character.isWhitespace(ch);
        }

        try (var streamReader = new InputStreamReader(inputStream, charset);
            var reader = new BufferedReader(streamReader))
        {
            buffer.clear();
            while (true)
            {
                var size = reader.read(buffer);
                if (size == -1)
                {
                    break;
                }

                var pos = 0;
                for (var i = 0; i < size; i++)
                {
                    var ch = buffer.get(i);
                    if (filter.test(ch))
                    {
                        if (i != pos)
                        {
                            buffer.put(pos, ch);
                        }

                        pos++;
                    }
                }

                buffer.position(pos);
                buffer.flip();
                var bytes = charset.encode(buffer);
                consumer.accept(bytes.array());
                buffer.clear();
            }
        }
    }

    @Override
    public MessageDigest compute(IFile file, CharBuffer buffer)
        throws UnsupportedEncodingException, IOException, CoreException
    {
        Charset charset;
        try
        {
            charset = Charset.forName(file.getCharset());
        }
        catch (CoreException e)
        {
            charset = StandardCharsets.UTF_8;
        }

        try (var inputStream = file.getContents(true);)
        {
            return compute(inputStream, charset, buffer);
        }
    }

    @Override
    public MessageDigest compute(InputStream inputStream, Charset charset, CharBuffer buffer)
        throws UnsupportedEncodingException, IOException
    {
        var hash = messageDigestProvider.get();
        scanTextStream(buffer, bytes -> hash.update(bytes), inputStream, charset, null);
        return hash;
    }
}
