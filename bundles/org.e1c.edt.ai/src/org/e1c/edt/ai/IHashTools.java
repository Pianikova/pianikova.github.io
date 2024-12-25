/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface IHashTools
{
    MessageDigest clone(MessageDigest hash);

    String format(MessageDigest hash);

    void scanTextStream(CharBuffer buffer, Consumer<byte[]> consumer, InputStream inputStream, Charset charset,
        Predicate<Character> filter)
        throws UnsupportedEncodingException, IOException;
}
